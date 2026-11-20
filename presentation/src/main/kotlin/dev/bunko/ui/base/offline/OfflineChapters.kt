/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.offline

import dev.bunko.ui.reader.toReaderFailure
import dev.bunko.ui.reader.message
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.chapter.interactor.GetChapterPages
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.server.Http
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.Length
import com.diamondedge.logging.logging
import dev.bunko.domain.server.MAX_PAGE_BYTES
import dev.bunko.domain.server.model.serverResource
import dev.bunko.domain.server.readBytesCapped
import io.ktor.client.request.prepareGet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

sealed interface OfflineState {
    data class Downloading(
        val progress: Float,
    ) : OfflineState

    data object Downloaded : OfflineState
}

class OfflineChapters(
    val store: OfflineChapterStore,
    private val getChapterPages: GetChapterPages,
    private val http: Http,
    private val serverPreferences: ServerPreferences,
    private val contextWrapper: ContextWrapper,
) {
    private class Running(
        val mangaId: Long,
        val job: Job,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val chapterGate = Semaphore(1)
    private val pageGate = Semaphore(PAGE_WORKERS)
    private val jobs = MutableStateFlow<Map<Long, Running>>(emptyMap())
    private val cleanups = MutableStateFlow<Set<Job>>(emptySet())

    val active: StateFlow<Boolean> = jobs.map { it.isNotEmpty() }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _states = MutableStateFlow<Map<Long, OfflineState>>(emptyMap())
    val states: StateFlow<Map<Long, OfflineState>> = _states.asStateFlow()

    init {
        scope.launch {
            val onDisk = store.all().associate { it.chapter.id to OfflineState.Downloaded }
            _states.update { onDisk + it }
        }
    }

    fun download(
        manga: Manga,
        chapters: List<Chapter>,
    ) {
        claim(chapters).forEach { chapter ->
            val job = scope.launch(start = CoroutineStart.LAZY) { run(manga, chapter) }
            jobs.update { it + (chapter.id to Running(manga.id, job)) }
            job.start()
        }
    }

    private fun claim(chapters: List<Chapter>): List<Chapter> {
        var claimed = emptyList<Chapter>()
        _states.update { current ->
            claimed = chapters.distinctBy { it.id }.filter { it.id !in current }
            current + claimed.associate { it.id to OfflineState.Downloading(0f) }
        }
        return claimed
    }

    fun delete(chapters: List<Chapter>) {
        chapters.forEach { chapter ->
            jobs.value[chapter.id]?.job?.cancel()
            _states.update { it - chapter.id }
            cleanup { store.delete(chapter.mangaId, chapter.id) }
        }
    }

    fun deleteAll() {
        jobs.value.values.forEach { it.job.cancel() }
        _states.value = emptyMap()
        cleanup { store.clear() }
    }

    private fun cleanup(block: () -> Unit) {
        val job = scope.launch(start = CoroutineStart.LAZY) { chapterGate.withPermit { block() } }
        cleanups.update { it + job }
        job.invokeOnCompletion { cleanups.update { pending -> pending - job } }
        job.start()
    }

    private suspend fun run(
        manga: Manga,
        chapter: Chapter,
    ) {
        try {
            cleanups.value.joinAll()
            chapterGate.withPermit {
                try {
                    saveCover(manga)
                    saveChapter(manga.title, chapter)
                    setTracked(chapter.id, OfflineState.Downloaded)
                } catch (e: Exception) {
                    e.throwIfCancellation()
                    log.warn(e) { "Offline download failed for ${chapter.id}" }
                    _states.update { it - chapter.id }
                    pruneIfOrphaned(chapter)
                    toastOnce(contextWrapper.toPlatformString(e.toReaderFailure().message))
                }
            }
        } finally {
            // a delete + re-download may already have replaced this entry with a newer job
            val self = coroutineContext.job
            jobs.update { if (it[chapter.id]?.job === self) it - chapter.id else it }
        }
    }

    private fun setTracked(
        chapterId: Long,
        state: OfflineState,
    ) = _states.update { if (chapterId in it) it + (chapterId to state) else it }

    private suspend fun saveChapter(
        mangaTitle: String,
        chapter: Chapter,
    ) {
        val urls = getChapterPages.asFlow(chapter.id).first()
        check(urls.isNotEmpty()) { "No pages for ${chapter.name}" }
        store.sweepTemp(chapter.mangaId, chapter.id)

        val done = MutableStateFlow(0)
        coroutineScope {
            urls.mapIndexed { index, url ->
                async {
                    pageGate.withPermit { fetchPage(chapter, index, url) }
                    val n = done.updateAndGet { it + 1 }
                    setTracked(chapter.id, OfflineState.Downloading(n / urls.size.toFloat()))
                }
            }.awaitAll()
        }
        store.commit(OfflineChapterMeta(mangaTitle, urls.size, chapter))
    }

    private suspend fun saveCover(manga: Manga) {
        if (store.coverPath(manga.id) != null) return
        val path = manga.thumbnailUrl?.takeIf { it.isNotBlank() } ?: return
        runCatching {
            val url = serverResource(serverPreferences.serverUrl().get().toString(), path)
            val bytes = http.value.prepareGet(url).execute { it.readBytesCapped(MAX_PAGE_BYTES) }
            store.writeCover(manga.id, bytes)
        }.throwIfCancellation().onFailure { log.warn(it) { "Offline cover not saved for ${manga.id}" } }
    }

    private fun pruneIfOrphaned(chapter: Chapter) {
        val siblingQueued = jobs.value.any { (id, running) -> id != chapter.id && running.mangaId == chapter.mangaId }
        if (!siblingQueued) store.deleteSeriesIfEmpty(chapter.mangaId)
    }

    private suspend fun fetchPage(
        chapter: Chapter,
        index: Int,
        url: String,
    ) {
        if (store.hasPage(chapter.mangaId, chapter.id, index)) return
        var attempt = 0
        while (true) {
            try {
                val bytes = getChapterPages.asFlow(url) {}.first()
                store.writePage(chapter.mangaId, chapter.id, index, bytes)
                return
            } catch (e: Exception) {
                e.throwIfCancellation()
                if (++attempt >= PAGE_ATTEMPTS) throw e
                delay(RETRY_DELAY_MS * attempt)
            }
        }
    }

    private var lastToast: Pair<String, TimeMark>? = null

    @Synchronized
    private fun toastOnce(message: String) {
        val last = lastToast
        if (last != null && last.first == message && last.second.elapsedNow() < TOAST_WINDOW) return
        lastToast = message to TimeSource.Monotonic.markNow()
        contextWrapper.toast(message, Length.SHORT)
    }

    private companion object {
        const val PAGE_WORKERS = 3
        const val PAGE_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1_000L
        val TOAST_WINDOW = 30.seconds
        val log = logging()
    }
}
