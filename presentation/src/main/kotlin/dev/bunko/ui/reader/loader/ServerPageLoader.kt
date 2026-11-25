/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader.loader

import okio.IOException
import dev.bunko.ui.reader.toReaderFailure
import dev.bunko.ui.reader.ReaderFailure
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.unit.IntSize
import dev.bunko.core.io.source
import dev.bunko.core.lang.PriorityChannel
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.chapter.interactor.GetChapterPages
import dev.bunko.domain.reader.service.ReaderPreferences
import dev.bunko.domain.server.Http
import dev.bunko.ui.base.image.BitmapDecoderFactory
import dev.bunko.ui.base.model.StableHolder
import dev.bunko.ui.base.offline.OfflineChapterStore
import dev.bunko.ui.reader.model.ReaderChapter
import dev.bunko.ui.reader.model.ReaderPage
import cafe.adriel.voyager.core.concurrent.AtomicInt32
import com.diamondedge.logging.logging
import com.seiko.imageloader.asImageBitmap
import com.seiko.imageloader.cache.disk.DiskCache
import com.seiko.imageloader.component.decoder.DecodeResult
import com.seiko.imageloader.model.ImageResult
import com.seiko.imageloader.model.ImageSource
import com.seiko.imageloader.model.ImageSourceFrom
import com.seiko.imageloader.option.Options
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import okio.BufferedSource
import okio.FileSystem
import okio.buffer
import okio.use

class ServerPageLoader(
    val chapter: ReaderChapter,
    readerPreferences: ReaderPreferences,
    private val http: Http,
    private val chapterCache: DiskCache,
    private val offlineStore: OfflineChapterStore,
    private val bitmapDecoderFactory: BitmapDecoderFactory,
    private val getChapterPages: GetChapterPages,
) : PageLoader() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val channel = PriorityChannel<PriorityPage>(
        scope = scope,
        comparator = { i1, i2 -> i1.compareTo(i2) },
    )

    private val preloadSize = readerPreferences.preload().stateIn(scope)

    private val wanted = MutableStateFlow<Set<Int>>(emptySet())

    private val active = MutableStateFlow<Map<Int, Job>>(emptyMap())

    private val pagesFlow by lazy {
        MutableStateFlow<PagesState>(PagesState.Loading)
    }

    init {
        readerPreferences.threads()
            .stateIn(scope)
            .mapLatest {
                coroutineScope {
                    repeat(it) {
                        launch {
                            for (priorityPage in channel) {
                                try {
                                    val page = priorityPage.page
                                    if (page.index2 !in wanted.value) continue
                                    if (page.status.compareAndSet(ReaderPage.Status.QUEUE, ReaderPage.Status.WORKING)) fetchTracked(page)
                                } catch (e: Exception) {
                                    e.throwIfCancellation()
                                    log.warn(e) { "Error in loop" }
                                }
                            }
                        }
                    }
                }
            }
            .launchIn(scope)
    }

    private suspend fun fetchTracked(page: ReaderPage) =
        coroutineScope {
            val job = launch { fetchImage(page) }
            active.update { it + (page.index2 to job) }
            try {
                job.join()
            } finally {
                active.update { if (it[page.index2] === job) it - page.index2 else it }
                if (job.isCancelled && page.status.value == ReaderPage.Status.WORKING) {
                    page.status.value = ReaderPage.Status.QUEUE
                    page.progress.value = 0F
                }
            }
        }

    private val offline by lazy { offlineStore.isDownloaded(chapter.chapter.mangaId, chapter.chapter.id) }

    private suspend fun isCached(page: ReaderPage): Boolean =
        withContext(Dispatchers.IO) {
            chapterCache.openSnapshot(page.cacheKey)?.use { true } ?: false
        }

    private suspend fun fetchImage(page: ReaderPage) {
        if (offline) {
            val ready = withContext(Dispatchers.IO) {
                offlineStore.hasPage(chapter.chapter.mangaId, chapter.chapter.id, page.index2)
            }
            if (ready) {
                page.bitmap.value = StableHolder { getImageFromCache(page) }
                page.status.value = ReaderPage.Status.READY
                page.error.value = null
            } else {
                page.bitmap.value = StableHolder(null)
                page.status.value = ReaderPage.Status.ERROR
                page.error.value = ReaderFailure.OfflinePageMissing
            }
            return
        }
        if (isCached(page)) {
            log.debug { "Page ${page.index2} served from chapter cache" }
            page.bitmap.value = StableHolder { getImageFromCache(page) }
            page.status.value = ReaderPage.Status.READY
            page.error.value = null
            return
        }
        log.debug { "Loading page ${page.index2}" }
        flow {
            val response = getChapterPages.asFlow(page.url) {}

            emitAll(response)
        }
            .retryWhen { cause, attempt ->
                (cause is ServerResponseException && attempt < 1).also { if (it) delay(SERVER_RETRY_DELAY_MS) }
            }
            .onEach {
                putImageInCache(it, page)
                page.bitmap.value = StableHolder { getImageFromCache(page) }
                page.status.value = ReaderPage.Status.READY
                page.error.value = null
            }
            .catch {
                if (it is CancellationException) throw it
                page.bitmap.value = StableHolder(null)
                page.status.value = ReaderPage.Status.ERROR
                page.error.value = it.toReaderFailure()
                log.warn(it) { "Failed to get page ${page.index2} for chapter ${chapter.chapter.index} for ${chapter.chapter.mangaId}" }
            }
            .flowOn(Dispatchers.IO)
            .collect()
    }

    private suspend fun putImageInCache(
        response: ByteArray,
        page: ReaderPage,
    ) {
        val editor = chapterCache.openEditor(page.cacheKey)
            ?: throw Exception("Couldn't open cache")
        try {
            FileSystem.SYSTEM.write(editor.data) {
                response.source().use {
                    writeAll(it)
                }
            }
            editor.commit()
        } catch (e: Exception) {
            editor.abortQuietly()
            throw e
        }
    }

    private suspend fun getImageFromCache(page: ReaderPage): ReaderPage.ImageDecodeState {
        val result = decodeFromCache(page, gpu = true)
        if (result !is ReaderPage.ImageDecodeState.FailedToDecode) return result
        log.warn(result.exception) { "Retrying page ${page.index2} with a downscaled software bitmap" }
        return decodeFromCache(page, gpu = false)
    }

    private suspend fun decodeFromCache(
        page: ReaderPage,
        gpu: Boolean,
    ): ReaderPage.ImageDecodeState =
        runCatching {
            if (offline) {
                offlineStore.pagePath(chapter.chapter.mangaId, chapter.chapter.id, page.index2)
                    ?.let { decode(FileSystem.SYSTEM.source(it).buffer(), gpu, page) }
            } else {
                chapterCache.openSnapshot(page.cacheKey)?.use { decode(it.source(), gpu, page) }
            }
        }.throwIfCancellation()
            .onFailure { log.warn(it) { "Page ${page.index2} could not be opened" } }
            .getOrNull()
            ?: ReaderPage.ImageDecodeState.FailedToGetSnapShot

    private suspend fun decode(
        source: BufferedSource,
        gpu: Boolean,
        page: ReaderPage,
    ): ReaderPage.ImageDecodeState =
        source.use {
            val longSide = decodedLongSide(source, if (gpu) MAX_PIXELS else FALLBACK_MAX_PIXELS)
            val options = Options {
                imageBitmapConfig = if (gpu && longSide <= MAX_GPU_SIDE) ImageBitmapConfig.Gpu else ImageBitmapConfig.Argb8888
                maxImageSize = longSide
            }
            val decoder = bitmapDecoderFactory.create(
                ImageResult.OfSource(ImageSource(source), ImageSourceFrom.Disk),
                options,
            ) ?: return@use ReaderPage.ImageDecodeState.UnknownDecoder
            runCatching { decoder.decode() as DecodeResult.OfBitmap }
                .mapCatching {
                    ReaderPage.ImageDecodeState.Success(
                        it.bitmap.asImageBitmap().also {
                            page.bitmapInfo.value = ReaderPage.BitmapInfo(IntSize(it.width, it.height))
                        },
                    )
                }
                .getOrElse { ReaderPage.ImageDecodeState.FailedToDecode(it) }
        }

    private fun decodedLongSide(source: BufferedSource, maxPixels: Double): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        source.peek().inputStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return Options.DEFAULT_MAX_IMAGE_SIZE
        val scale = min(1.0, sqrt(maxPixels / (width.toDouble() * height)))
        val side = ceil(max(width, height) * scale).toInt().coerceAtLeast(1)
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) min(side, MAX_GPU_SIDE) else side
    }

    private fun preloadAround(
        currentPage: ReaderPage,
        amount: Int,
    ) {
        val pages = (pagesFlow.value as? PagesState.Success)?.pages.orEmpty()
        val pageIndex = currentPage.index2.coerceIn(0, pages.size)
        val ahead = pages.subList((pageIndex + 1).coerceAtMost(pages.size), (pageIndex + 1 + amount).coerceAtMost(pages.size))
        val behind = pages.subList((pageIndex - PRELOAD_BEHIND).coerceAtLeast(0), pageIndex).asReversed()
        setWanted((ahead + behind).mapTo(mutableSetOf(currentPage.index2)) { it.index2 })
        enqueue(ahead, 0)
        enqueue(behind, -1)
    }

    private fun setWanted(indices: Set<Int>) {
        wanted.value = indices
        active.value.forEach { (index, job) ->
            if (index !in indices) job.cancel()
        }
    }

    override fun cancelPending() = setWanted(emptySet())

    override suspend fun <T> readPage(page: ReaderPage, block: (BufferedSource) -> T): T? =
        withContext(Dispatchers.IO) {
            if (offline) {
                offlineStore.pagePath(chapter.chapter.mangaId, chapter.chapter.id, page.index2)?.let { FileSystem.SYSTEM.source(it).buffer().use(block) }
            } else {
                chapterCache.openSnapshot(page.cacheKey)?.use { block(it.source()) }
            }
        }

    private fun enqueue(
        pages: List<ReaderPage>,
        priority: Int,
    ) {
        pages.forEach {
            if (it.status.value == ReaderPage.Status.QUEUE) {
                scope.launch { channel.send(PriorityPage(it, priority)) }
            }
        }
    }

    override fun preloadFirstPages(count: Int) {
        val pages = (pagesFlow.value as? PagesState.Success)?.pages ?: return
        val first = pages.take(count)
        wanted.update { wanted -> wanted + first.map { it.index2 } }
        enqueue(first, 0)
    }

    override fun getPages(): StateFlow<PagesState> {
        scope.launch {
            if (pagesFlow.value != PagesState.Loading) return@launch
            val pages = if (offline) {
                val meta = withContext(Dispatchers.IO) { offlineStore.readMeta(chapter.chapter.mangaId, chapter.chapter.id) }
                List(meta?.pageCount ?: 0) { "offline/$it" }
            } else {
                getChapterPages.await(chapter.chapter.id)
            }
            pagesFlow.value = if (pages.isNullOrEmpty()) {
                PagesState.Empty
            } else {
                PagesState.Success(
                    pages.mapIndexed { index, url ->
                        ReaderPage(
                            url = url,
                            index2 = index,
                            bitmap = MutableStateFlow(StableHolder(null)),
                            bitmapInfo = MutableStateFlow(null),
                            progress = MutableStateFlow(0.0F),
                            status = MutableStateFlow(ReaderPage.Status.QUEUE),
                            error = MutableStateFlow(null),
                            chapter = chapter,
                        )
                    },
                )
            }
        }
        return pagesFlow.asStateFlow()
    }

    override fun loadPage(page: ReaderPage) {
        scope.launch {
            if (page.status.value == ReaderPage.Status.ERROR) {
                page.status.value = ReaderPage.Status.QUEUE
            }

            preloadAround(page, preloadSize.value)
            if (page.status.value == ReaderPage.Status.QUEUE) {
                channel.send(PriorityPage(page, 1))
            }
        }
    }

    override fun retryPage(page: ReaderPage) {
        wanted.update { it + page.index2 }
        scope.launch {
            if (page.status.value == ReaderPage.Status.ERROR || page.status.value == ReaderPage.Status.READY) {
                if (!offline) withContext(Dispatchers.IO) { chapterCache.remove(page.cacheKey) }
                page.status.value = ReaderPage.Status.QUEUE
            }
            channel.send(PriorityPage(page, 2))
        }
    }

    private class PriorityPage(
        val page: ReaderPage,
        val priority: Int,
    ) : Comparable<PriorityPage> {
        companion object {
            private val idGenerator = AtomicInt32(1)
        }

        private val identifier = idGenerator.getAndIncrement()

        override fun compareTo(other: PriorityPage): Int {
            val p = other.priority.compareTo(priority)
            return if (p != 0) p else identifier.compareTo(other.identifier)
        }
    }

    override fun recycle() {
        super.recycle()
        wanted.value = emptySet()
        scope.cancel()
        channel.close()
    }

    private companion object {
        private const val PRELOAD_BEHIND = 2
        private const val SERVER_RETRY_DELAY_MS = 400L
        private const val MAX_PIXELS = 16_000_000.0
        private const val FALLBACK_MAX_PIXELS = 4_000_000.0
        private const val MAX_GPU_SIDE = 8192
        private val log = logging()
    }

    private val ReaderPage.cacheKey
        get() = "${chapter.chapter.id}-$url"

    private fun DiskCache.Snapshot.source(): BufferedSource = FileSystem.SYSTEM.source(data).buffer()

    private fun DiskCache.Editor.abortQuietly() {
        try {
            abort()
        } catch (_: IOException) {
        }
    }
}
