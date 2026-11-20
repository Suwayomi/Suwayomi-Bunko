/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.history

import com.diamondedge.logging.logging
import dev.bunko.domain.ServerListeners
import dev.bunko.domain.history.ReadHistory
import dev.bunko.domain.updates.model.MangaAndChapter
import dev.bunko.ui.base.offline.OfflineChapters
import dev.bunko.ui.base.userMessage
import dev.bunko.ui.reader.ChapterProgress
import dev.bunko.ui.reader.ReaderProgressSync
import dev.bunko.ui.reader.withProgress
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@Inject
class ReadHistoryViewModel(
    private val readHistory: ReadHistory,
    private val progressSync: ReaderProgressSync,
    private val offlineChapters: OfflineChapters,
    serverListeners: ServerListeners,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    sealed interface State {
        data object Loading : State

        data class Failed(val message: String) : State

        data class Loaded(
            val pending: List<HistoryEntry>,
            val days: List<HistoryDay>,
            val hasMore: Boolean,
        ) : State {
            val isEmpty get() = pending.isEmpty() && days.isEmpty()
        }
    }

    private sealed interface Server {
        data object Loading : Server

        data class Failed(val message: String) : Server

        data class Loaded(
            val clearedAt: Long,
            val entries: List<MangaAndChapter>,
            val offset: Int,
            val hasMore: Boolean,
        ) : Server
    }

    private val server = MutableStateFlow<Server>(Server.Loading)
    private var loadJob: Job? = null

    val state: StateFlow<State> = combine(server, progressSync.queued) { server, queued -> server to queued }
        .map { (server, queued) -> withContext(Dispatchers.IO) { present(server, queued) } }
        .stateIn(scope, SharingStarted.Eagerly, State.Loading)

    init {
        reload()
        merge(
            serverListeners.mangaListener.map { },
            serverListeners.libraryUpdated,
            progressSync.queued.map { it.keys }.distinctUntilChanged().drop(1).map { },
        )
            .debounce(RELOAD_DEBOUNCE)
            .onEach { reload(keepLoaded = true) }
            .launchIn(scope)
    }

    fun reload(keepLoaded: Boolean = false) {
        val current = server.value
        val count = if (keepLoaded && current is Server.Loaded) maxOf(current.offset, ReadHistory.PAGE_SIZE) else ReadHistory.PAGE_SIZE
        if (!keepLoaded) server.value = Server.Loading
        loadJob?.cancel()
        loadJob = scope.launch {
            val result = readHistory.clearedAt().mapCatching { clearedAt ->
                val page = readHistory.page(clearedAt, offset = 0, count = count).getOrThrow()
                Server.Loaded(clearedAt, page.entries, count, page.hasNextPage)
            }
            result
                .onSuccess { server.value = it }
                .onFailure { e ->
                    log.warn(e) { "Read history failed to load" }
                    if (server.value !is Server.Loaded) server.value = Server.Failed(e.userMessage.toPlatformString())
                }
        }
    }

    fun loadMore() {
        val current = server.value as? Server.Loaded ?: return
        if (!current.hasMore || loadJob?.isActive == true) return
        loadJob = scope.launch {
            readHistory.page(current.clearedAt, current.offset)
                .onSuccess { page ->
                    server.value =
                        Server.Loaded(current.clearedAt, (current.entries + page.entries).distinctBy { it.chapter.id }, current.offset + ReadHistory.PAGE_SIZE, page.hasNextPage)
                }
                .onFailure { toast(it.userMessage.toPlatformString()) }
        }
    }

    fun remove(entry: HistoryEntry) {
        val current = server.value as? Server.Loaded ?: return
        server.value = current.copy(entries = current.entries.filterNot { it.chapter.id == entry.chapter.id })
        scope.launch {
            readHistory.remove(entry.chapter).onFailure {
                toast(it.userMessage.toPlatformString())
                reload(keepLoaded = true)
            }
        }
    }

    fun clear() {
        val current = server.value as? Server.Loaded ?: return
        val newest = current.entries.firstOrNull()?.chapter?.lastReadAt ?: return
        server.value = current.copy(entries = emptyList(), hasMore = false)
        scope.launch {
            readHistory.clear(newest)
                .onSuccess { reload() }
                .onFailure {
                    toast(it.userMessage.toPlatformString())
                    reload()
                }
        }
    }

    private fun present(
        server: Server,
        queued: Map<Long, ChapterProgress>,
    ): State =
        when (server) {
            Server.Loading -> State.Loading
            is Server.Failed -> State.Failed(server.message)
            is Server.Loaded -> {
                val (pendingRows, syncedRows) = server.entries.partition { it.chapter.id in queued }
                val pending = pendingRows.map { it.toEntry(queued, pending = true) } +
                    offlineOnly(queued.values.filter { p -> server.entries.none { it.chapter.id == p.chapterId } })
                State.Loaded(
                    pending = pending,
                    days = syncedRows.map { it.toEntry(queued, pending = false) }.groupByDay(),
                    hasMore = server.hasMore,
                )
            }
        }

    private fun offlineOnly(progress: List<ChapterProgress>): List<HistoryEntry> =
        progress.mapNotNull { p ->
            val meta = offlineChapters.store.readMeta(p.mangaId, p.chapterId) ?: return@mapNotNull null
            HistoryEntry(
                mangaTitle = meta.mangaTitle,
                cover = offlineChapters.store.coverPath(p.mangaId)?.toFile(),
                chapter = meta.chapter.withProgress(p),
                readAt = null,
                pending = true,
            )
        }

    private fun MangaAndChapter.toEntry(
        queued: Map<Long, ChapterProgress>,
        pending: Boolean,
    ) = HistoryEntry(
        mangaTitle = manga.title,
        cover = manga,
        chapter = chapter.withProgress(queued[chapter.id]),
        readAt = chapter.lastReadAt,
        pending = pending,
    )

    private companion object {
        val RELOAD_DEBOUNCE = 2.seconds
        val log = logging()
    }
}
