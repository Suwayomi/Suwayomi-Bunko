/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.schedule

import com.diamondedge.logging.logging
import dev.bunko.core.di.AppScope
import dev.bunko.domain.library.service.LibraryRepository
import dev.bunko.domain.manga.model.Manga
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

data class ScheduledSeries(
    val manga: Manga,
    val categoryIds: List<Long>,
    val estimate: ReleaseEstimate,
)

@AppScope
@Inject
class GetReleaseSchedule(
    private val libraryRepository: LibraryRepository,
    private val statusStore: AniListStatusStore,
) {
    private val aniList by lazy {
        AniListStatuses(
            HttpClient(OkHttp) {
                install(HttpTimeout) {
                    connectTimeoutMillis = 10.seconds.inWholeMilliseconds
                    requestTimeoutMillis = 20.seconds.inWholeMilliseconds
                }
            },
            Json { ignoreUnknownKeys = true },
        )
    }

    private val cacheLock = Mutex()
    private var cached: Map<Long, StoredStatus>? = null

    suspend fun await(refresh: Boolean = false): Result<List<ScheduledSeries>> =
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            val histories = libraryRepository.getReleaseHistory(since = now - WINDOW.inWholeMilliseconds).first()
            val statuses = aniListStatuses(histories, now, refresh)
            Result.success(histories.map { ScheduledSeries(it.manga, it.categoryIds, estimateRelease(it, statuses[it.aniListId], now)) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun aniListStatuses(
        histories: List<ReleaseHistory>,
        now: Long,
        refresh: Boolean,
    ): Map<Long, AniListStatus> {
        val ids = histories.mapNotNull { it.aniListId }.distinct()
        if (ids.isEmpty()) return emptyMap()
        return cacheLock.withLock {
            var entries = cached ?: withContext(Dispatchers.IO) { runCatching { statusStore.load() }.getOrDefault(emptyMap()) }
            val stale = ids.filter { id -> refresh || entries[id]?.let { now - it.fetchedAt >= STATUS_TTL.inWholeMilliseconds } != false }
            if (stale.isNotEmpty()) {
                try {
                    val found = aniList.lookup(stale)
                    entries = entries + stale.associateWith { StoredStatus(found[it], now) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.warn(e) { "AniList status lookup failed, estimating from chapters only" }
                }
            }
            val current = entries.filterKeys { it in ids }
            if (current != cached) {
                withContext(Dispatchers.IO) { runCatching { statusStore.save(current) }.onFailure { log.warn(it) { "AniList statuses not saved" } } }
            }
            cached = current
            current.mapNotNull { (id, entry) -> entry.status?.let { id to it } }.toMap()
        }
    }

    private companion object {
        val WINDOW = 365.days
        val STATUS_TTL = 6.hours
        val log = logging()
    }
}
