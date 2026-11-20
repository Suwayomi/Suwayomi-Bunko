/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dev.bunko.core.di.AppScope
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.chapter.interactor.UpdateChapter
import dev.bunko.domain.chapter.interactor.UpdateChapterMeta
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.server.ConnectionError
import dev.bunko.domain.server.ServerRejectedException
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.server.toConnectionError
import dev.bunko.domain.user.service.UserPreferences
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.uicore.vm.ContextWrapper
import com.diamondedge.logging.logging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class SyncStatus(
    val queued: Int,
    val offline: Boolean,
    val authNeeded: Boolean,
)

@AppScope
@Inject
class ReaderProgressSync(
    private val updateChapter: UpdateChapter,
    private val updateChapterMeta: UpdateChapterMeta,
    private val uiPreferences: UiPreferences,
    private val userPreferences: UserPreferences,
    private val serverPreferences: ServerPreferences,
    contextWrapper: ContextWrapper,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store = PendingProgressStore(contextWrapper.filesDir.toOkioPath() / QUEUE_FILE, FileSystem.SYSTEM)
    private val pending = MutableStateFlow<Map<Long, ChapterProgress>>(emptyMap())
    private val unreachable = MutableStateFlow(false)
    private val authNeeded = MutableStateFlow(false)
    val status: StateFlow<SyncStatus> = combine(pending, unreachable, authNeeded) { queue, offline, auth ->
        SyncStatus(queue.size, offline && !auth && queue.isNotEmpty(), auth && queue.isNotEmpty())
    }.stateIn(scope, SharingStarted.Eagerly, SyncStatus(0, false, false))
    internal val queued: StateFlow<Map<Long, ChapterProgress>> = pending.asStateFlow()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val networkBack = Channel<Unit>(Channel.CONFLATED)
    private val loaded = CompletableDeferred<Unit>()

    init {
        scope.launch {
            pending.update { loadQueue() + it }
            loaded.complete(Unit)
            wake.trySend(Unit)
            pending.collect { persist(it.values) }
        }
        scope.launch {
            for (signal in wake) drain()
        }
        scope.launch { watchCredentials() }
        watchNetwork(contextWrapper)
    }

    private suspend fun watchCredentials() {
        combine(
            userPreferences.uiAccessToken().stateIn(scope),
            userPreferences.simpleSession().stateIn(scope),
            serverPreferences.authUsername().stateIn(scope),
            serverPreferences.authPassword().stateIn(scope),
        ) { token, session, username, password -> listOf(token, session, username, password) }
            .drop(1)
            .collect {
                if (authNeeded.value) {
                    authNeeded.value = false
                    wake.trySend(Unit)
                }
            }
    }

    private fun watchNetwork(contextWrapper: ContextWrapper) {
        val connectivity = contextWrapper.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        runCatching {
            connectivity.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        networkBack.trySend(Unit)
                    }
                },
            )
        }.onFailure { log.warn(it) { "Network callback not registered, relying on backoff" } }
    }

    suspend fun withPending(chapters: List<Chapter>): List<Chapter> {
        loaded.await()
        val queue = pending.value
        return chapters.map { it.withProgress(queue[it.id]) }
    }

    fun markRead(chapter: Chapter) = submit(chapter) { it.copy(read = true) }

    fun lastPageRead(
        chapter: Chapter,
        page: Int,
    ) = submit(chapter) { it.copy(lastPageRead = page) }

    fun pageOffset(
        chapter: Chapter,
        offset: Int,
    ) = submit(chapter) { it.copy(pageOffset = offset) }

    fun remove(chapterIds: Collection<Long>) {
        if (chapterIds.isEmpty()) return
        pending.update { it - chapterIds.toSet() }
    }

    private fun submit(
        chapter: Chapter,
        change: (ChapterProgress) -> ChapterProgress,
    ) {
        if (uiPreferences.incognito().get()) return
        pending.update { it + (chapter.id to change(it[chapter.id] ?: ChapterProgress(chapter.id, chapter.mangaId))) }
        wake.trySend(Unit)
    }

    private fun loadQueue(): Map<Long, ChapterProgress> =
        runCatching { store.load() }
            .onFailure { log.warn(it) { "Pending progress queue unreadable, starting empty" } }
            .getOrDefault(emptyMap())

    private fun persist(queue: Collection<ChapterProgress>) {
        runCatching { store.save(queue) }
            .onFailure { log.warn(it) { "Pending progress queue not saved" } }
    }

    private suspend fun drain() {
        var backoff = MIN_BACKOFF
        while (pending.value.isNotEmpty()) {
            networkBack.tryReceive()
            val failures = pending.value.values.mapNotNull { progress ->
                val failure = send(progress).exceptionOrNull()
                if (failure == null || failure is ServerRejectedException) {
                    if (failure != null) log.warn(failure) { "Server rejected progress for ${progress.chapterId}, dropping it" }
                    pending.update { if (it[progress.chapterId] === progress) it - progress.chapterId else it }
                    null
                } else {
                    failure
                }
            }
            authNeeded.value = failures.any { it.toConnectionError() is ConnectionError.Unauthorized }
            if (authNeeded.value) {
                log.warn(failures.first()) { "Progress sync paused, server refused the credentials" }
                unreachable.value = false
                return
            }
            unreachable.value = failures.isNotEmpty()
            if (failures.isEmpty()) {
                backoff = MIN_BACKOFF
                continue
            }
            log.warn(failures.first()) { "Progress sync deferred for ${failures.size} chapter(s), retrying in $backoff" }
            backoff = if (withTimeoutOrNull(backoff) { networkBack.receive() } != null) {
                MIN_BACKOFF
            } else {
                (backoff * 2).coerceAtMost(MAX_BACKOFF)
            }
        }
    }

    private suspend fun send(progress: ChapterProgress): Result<Unit> =
        runCatching {
            if (progress.read != null || progress.lastPageRead != null) {
                updateChapter.asFlow(progress.chapterId, progress.mangaId, read = progress.read, lastPageRead = progress.lastPageRead).collect()
            }
            if (progress.pageOffset != null) {
                updateChapterMeta.asFlow(progress.chapterId, progress.mangaId, progress.pageOffset).collect()
            }
        }.throwIfCancellation()

    private companion object {
        const val QUEUE_FILE = "pending_progress"
        val MIN_BACKOFF: Duration = 5.seconds
        val MAX_BACKOFF: Duration = 1.minutes
        val log = logging()
    }
}
