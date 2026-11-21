package dev.bunko.data.util

import android.content.Context
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.doNotStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.exception.CacheMissException
import com.diamondedge.logging.logging
import dev.bunko.domain.server.CredentialOrigins
import dev.bunko.domain.server.model.Auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flow
import java.io.File
import java.security.MessageDigest
import kotlin.reflect.KClass

private val log = logging("ApolloCache")

private const val CACHE_VERSION = 1
private const val CACHE_PREFIX = "apollo_"
private val SQLITE_SUFFIXES = listOf("", "-wal", "-shm", "-journal")
private const val PROBE_KEY = "bunko:probe"
private const val MAX_RECOVERIES = 1
private const val FALLBACK_MEMORY_BYTES = 8 * 1024 * 1024

internal fun cacheFileName(
    origins: CredentialOrigins,
    auth: Auth,
    username: String,
): String {
    val identity = listOf(CACHE_VERSION.toString(), origins.primary, origins.fallback, auth.toString(), username)
        .joinToString("\u0000") { it.orEmpty() }
    val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
    return CACHE_PREFIX + digest.take(16).joinToString("") { "%02x".format(it) } + ".db"
}

internal fun Context.deleteApolloCache(fileName: String) {
    SQLITE_SUFFIXES.forEach { suffix ->
        runCatching { File(noBackupFilesDir, fileName + suffix).delete() }
    }
}

private fun Context.pruneApolloCaches(keep: String) {
    noBackupFilesDir.listFiles()
        ?.filter { it.name.startsWith(CACHE_PREFIX) && !it.name.startsWith(keep) }
        ?.forEach { runCatching { it.delete() } }
}

internal fun apolloCacheFactory(
    context: Context,
    fileName: String,
): NormalizedCacheFactory =
    object : NormalizedCacheFactory() {
        override fun create(): NormalizedCache =
            ResilientNormalizedCache(
                open = {
                    context.pruneApolloCaches(fileName)
                    SqlNormalizedCacheFactory(context, fileName, useNoBackupDirectory = true).create()
                },
                wipe = { context.deleteApolloCache(fileName) },
            )
    }

internal fun <D : Query.Data> ApolloCall<D>.toFlowWithCache(cacheFirst: Boolean): Flow<ApolloResponse<D>> {
    val call = doNotStore(false)
    return if (cacheFirst) {
        call.fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().dropCacheMisses().keepCachedOnFailure()
    } else {
        call.toFlow()
    }
}

internal fun <D : Operation.Data> Flow<ApolloResponse<D>>.keepCachedOnFailure(): Flow<ApolloResponse<D>> =
    flow {
        var delivered = false
        collect { response ->
            if (delivered && response.exception != null) {
                log.info { "Network refresh failed, keeping cached data" }
                return@collect
            }
            if (response.exception == null && !response.hasErrors()) delivered = true
            emit(response)
        }
    }

internal fun <D : Operation.Data> Flow<ApolloResponse<D>>.dropCacheMisses(): Flow<ApolloResponse<D>> =
    filterNot { it.exception is CacheMissException }

internal class ResilientNormalizedCache(
    private val open: () -> NormalizedCache,
    private val wipe: () -> Unit,
) : NormalizedCache() {
    private var delegate: NormalizedCache? = null
    private var recoveries = 0

    @Synchronized
    private fun <T> guarded(
        fallback: T,
        block: (NormalizedCache) -> T,
    ): T {
        val cache = delegate ?: openOrRecover().also { delegate = it }
        return try {
            block(cache)
        } catch (e: Exception) {
            log.warn(e) { "Apollo cache operation failed, resetting the cache" }
            delegate = recover()
            fallback
        }
    }

    private fun openChecked(): NormalizedCache = open().also { it.loadRecord(PROBE_KEY, CacheHeaders.NONE) }

    private fun openOrRecover(): NormalizedCache =
        try {
            openChecked()
        } catch (e: Exception) {
            log.warn(e) { "Apollo cache failed to open, resetting the cache" }
            recover()
        }

    private fun recover(): NormalizedCache {
        runCatching { wipe() }
        if (recoveries++ >= MAX_RECOVERIES) return memoryFallback()
        return try {
            openChecked()
        } catch (e: Exception) {
            log.warn(e) { "Apollo cache could not be recreated, using memory only" }
            runCatching { wipe() }
            memoryFallback()
        }
    }

    private fun memoryFallback(): NormalizedCache = MemoryCacheFactory(maxSizeBytes = FALLBACK_MEMORY_BYTES).create()

    override fun loadRecord(
        key: String,
        cacheHeaders: CacheHeaders,
    ): Record? = guarded(null) { it.loadRecord(key, cacheHeaders) }

    override fun loadRecords(
        keys: Collection<String>,
        cacheHeaders: CacheHeaders,
    ): Collection<Record> = guarded(emptyList()) { it.loadRecords(keys, cacheHeaders) }

    override fun dump(): Map<KClass<*>, Map<String, Record>> = guarded(emptyMap()) { it.dump() }

    override fun merge(
        record: Record,
        cacheHeaders: CacheHeaders,
    ): Set<String> = guarded(emptySet()) { it.merge(record, cacheHeaders) }

    override fun merge(
        records: Collection<Record>,
        cacheHeaders: CacheHeaders,
    ): Set<String> = guarded(emptySet()) { it.merge(records, cacheHeaders) }

    override fun clearAll() = guarded(Unit) { it.clearAll() }

    override fun remove(
        cacheKey: CacheKey,
        cascade: Boolean,
    ): Boolean = guarded(false) { it.remove(cacheKey, cascade) }

    override fun remove(pattern: String): Int = guarded(0) { it.remove(pattern) }
}
