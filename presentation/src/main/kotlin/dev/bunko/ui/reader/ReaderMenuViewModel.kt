/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import dev.bunko.ui.reader.message
import dev.bunko.core.lang.launchDefault
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.i18n.MR
import dev.bunko.domain.download.interactor.QueueChapterDownload
import dev.bunko.domain.reader.model.ImageSaveTarget
import dev.icerock.moko.resources.StringResource
import dev.bunko.core.prefs.getAsFlow
import dev.bunko.domain.chapter.interactor.GetChapterPages
import dev.bunko.domain.chapter.interactor.GetChapters
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.model.ChapterListOptions
import dev.bunko.domain.chapter.model.nextReadable
import dev.bunko.domain.chapter.model.previousReadable
import dev.bunko.domain.manga.interactor.GetManga
import dev.bunko.domain.manga.interactor.SetMangaMetaValue
import dev.bunko.domain.manga.interactor.UpdateMangaMeta
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.manga.model.MangaMeta
import dev.bunko.domain.reader.ReaderModeWatch
import dev.bunko.domain.reader.model.Direction
import dev.bunko.domain.reader.model.ImageScale
import dev.bunko.domain.reader.model.ReaderBackground
import dev.bunko.domain.reader.service.ReaderPreferences
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.domain.server.Http
import dev.bunko.ui.base.ChapterCache
import dev.bunko.ui.base.offline.OfflineChapters
import dev.bunko.ui.base.image.BitmapDecoderFactory
import dev.bunko.ui.base.model.StableHolder
import dev.bunko.ui.reader.loader.PagesState
import dev.bunko.ui.reader.model.MoveTo
import dev.bunko.ui.reader.model.Navigation
import dev.bunko.ui.reader.model.PageMove
import dev.bunko.ui.reader.model.ReaderChapter
import dev.bunko.ui.reader.model.ReaderItem
import dev.bunko.ui.reader.model.ReaderPage
import dev.bunko.ui.reader.model.ReaderPageSeparator
import dev.bunko.ui.reader.model.ViewerChapters
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import io.ktor.http.decodeURLQueryComponent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Assisted
import dev.bunko.domain.discord.DiscordPresence
import dev.bunko.domain.discord.model.ReadingActivity
import kotlin.time.Clock
import me.tatarka.inject.annotations.Inject

@Inject
class ReaderMenuViewModel(
    private val readerPreferences: ReaderPreferences,
    private val getManga: GetManga,
    private val getChapters: GetChapters,
    private val getChapterPages: GetChapterPages,
    private val updateMangaMeta: UpdateMangaMeta,
    private val setMangaMetaValue: SetMangaMetaValue,
    private val progressSync: ReaderProgressSync,
    private val imageStore: PageImageStore,
    private val queueChapterDownload: QueueChapterDownload,
    private val chapterCache: ChapterCache,
    private val offlineChapters: OfflineChapters,
    private val http: Http,
    private val discordPresence: DiscordPresence,
    uiPreferences: UiPreferences,
    contextWrapper: ContextWrapper,
    @Assisted private val params: Params,
) : ViewModel(contextWrapper) {
    override val scope = MainScope()
    private val manga = MutableStateFlow<Manga?>(null)
    private val mangaLoaded = CompletableDeferred<Unit>()
    val mangaTitle = manga.map { it?.title.orEmpty() }.stateIn(scope, SharingStarted.Eagerly, "")
    private val viewerChapters = MutableStateFlow(ViewerChapters(null, null, null))
    val previousChapter = viewerChapters.map { it.prevChapter }.stateIn(scope, SharingStarted.Eagerly, null)
    val chapter = viewerChapters.map { it.currChapter }.stateIn(scope, SharingStarted.Eagerly, null)
    val nextChapter = viewerChapters.map { it.nextChapter }.stateIn(scope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow<ReaderChapter.State>(ReaderChapter.State.Wait)
    val state = _state.asStateFlow()

    val pages = viewerChapters.flatMapLatest { viewerChapters ->
        val previousChapterPages = viewerChapters.prevChapter
            ?.pages
            ?.map { (it as? PagesState.Success)?.pages }
            ?: flowOf(null)
        val chapterPages = viewerChapters.currChapter
            ?.pages
            ?.map { (it as? PagesState.Success)?.pages }
            ?: flowOf(null)
        val nextChapterPages = viewerChapters.nextChapter
            ?.pages
            ?.map { (it as? PagesState.Success)?.pages }
            ?: flowOf(null)
        combine(previousChapterPages, chapterPages, nextChapterPages) { prev, cur, next ->
            (
                prev.orEmpty() +
                    ReaderPageSeparator(viewerChapters.prevChapter, viewerChapters.currChapter) +
                    cur.orEmpty() +
                    ReaderPageSeparator(viewerChapters.currChapter, viewerChapters.nextChapter) +
                    next.orEmpty()
            ).toImmutableList()
        }
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    private val _currentPage = MutableStateFlow<ReaderItem?>(null)
    val currentPage = _currentPage.asStateFlow()

    private val _currentPageOffset = MutableStateFlow(1)
    val currentPageOffset = _currentPageOffset.asStateFlow()

    private val _readerSettingsMenuOpen = MutableStateFlow(false)
    val readerSettingsMenuOpen = _readerSettingsMenuOpen.asStateFlow()

    private val _pageEmitter = MutableSharedFlow<PageMove>()
    val pageEmitter = StableHolder(_pageEmitter.asSharedFlow())

    val readerModes = readerPreferences.modes()
        .getAsFlow()
        .map { it.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())
    val readerMode = combine(readerPreferences.mode().getAsFlow(), manga) { mode, manga ->
        val mangaMode = manga?.meta?.bunkoReaderMode?.decodeURLQueryComponent()
        if (
            mangaMode != null &&
            mangaMode != MangaMeta.DEFAULT_READER_MODE &&
            mangaMode in readerModes.value
        ) {
            mangaMode
        } else {
            mode
        }
    }.stateIn(scope, SharingStarted.Eagerly, readerPreferences.mode().get())

    val readerModeSettings = ReaderModeWatch(readerPreferences, scope, readerMode)

    /** The mode picked for this series, or the default marker when it follows the global mode. */
    val seriesReaderMode = combine(manga, readerModes) { manga, modes ->
        manga?.meta?.bunkoReaderMode?.decodeURLQueryComponent()?.takeIf { it in modes } ?: MangaMeta.DEFAULT_READER_MODE
    }.stateIn(scope, SharingStarted.Eagerly, MangaMeta.DEFAULT_READER_MODE)

    val seriesOrientation = manga.map { manga ->
        manga?.meta?.orientation?.let { stored -> OrientationLock.entries.find { it.name == stored } }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun setSeriesOrientation(lock: OrientationLock) {
        toast(MR.strings.reader_orientation_changed.toPlatformString(lock.label.toPlatformString()))
        scope.launchDefault {
            // a free rotation is the default, so nothing needs storing for it
            setMangaMetaValue.await(
                params.mangaId,
                MangaMeta.ORIENTATION_KEY,
                lock.name.takeUnless { lock == OrientationLock.Unlocked },
                onError = { toast(it.toReaderFailure().message.toPlatformString()) },
            )
            initManga(params.mangaId)
        }
    }

    val volumeKeys = readerPreferences.volumeKeys().stateIn(scope)
    val volumeKeysInverted = readerPreferences.volumeKeysInverted().stateIn(scope)
    val tappingInvertMode = readerPreferences.tappingInvertMode().stateIn(scope)

    fun setVolumeKeys(enabled: Boolean) {
        readerPreferences.volumeKeys().set(enabled)
    }

    val dimmer = readerPreferences.dimmer().stateIn(scope)

    fun setDimmer(alpha: Float) {
        readerPreferences.dimmer().set(alpha.coerceIn(0f, MAX_DIMMER))
    }

    val background = readerPreferences.background().stateIn(scope)

    fun setBackground(background: ReaderBackground) {
        readerPreferences.background().set(background)
    }

    val autoScrollSpeed = readerPreferences.autoScrollSpeed().stateIn(scope)

    fun setAutoScrollSpeed(dpPerSecond: Int) {
        readerPreferences.autoScrollSpeed().set(dpPerSecond.coerceIn(AUTO_SCROLL_SPEEDS.first, AUTO_SCROLL_SPEEDS.last))
    }

    private val _autoScroll = MutableStateFlow(false)
    val autoScroll = _autoScroll.asStateFlow()

    fun setAutoScroll(enabled: Boolean) {
        _autoScroll.value = enabled
    }

    val incognito = uiPreferences.incognito().stateIn(scope)

    fun setImageScale(scale: ImageScale) {
        readerPreferences.getMode(readerMode.value).imageScale().set(scale)
    }

    private val loader = ChapterLoader(
        readerPreferences = readerPreferences,
        http = http,
        chapterCache = chapterCache,
        offlineStore = offlineChapters.store,
        bitmapDecoderFactory = BitmapDecoderFactory(contextWrapper),
        getChapterPages = getChapterPages,
    )

    private val readingSince = Clock.System.now().toEpochMilliseconds()

    init {
        init()
        combine(manga, chapter) { manga, chapter ->
            if (manga == null || chapter == null) return@combine null
            ReadingActivity(
                mangaId = manga.id,
                mangaTitle = manga.title,
                chapterName = chapter.chapter.name,
                chapterNumber = chapter.chapter.chapterNumber,
                startedAt = readingSince,
            )
        }
            .onEach { if (it != null) discordPresence.setReading(it) }
            .launchIn(scope)
    }

    fun init() {
        scope.launchDefault {
            launch { runCatching { initManga(params.mangaId) } }
            runCatching { initChapters(params.mangaId, params.chapterId) }
        }
    }

    init {
        scope.launchDefault {
            currentPage
                .filterIsInstance<ReaderPage>()
                .collectLatest { page ->
                    page.chapter.pageLoader?.loadPage(page)
                    if (page.chapter == chapter.value) {
                        val pages = page.chapter.pages.value as? PagesState.Success
                            ?: return@collectLatest
                        if ((page.index2 + 1) >= pages.pages.size) {
                            markChapterRead(page.chapter)
                        }
                        val nextChapter = nextChapter.value
                        if (nextChapter != null && (page.index2 + 1) >= ((pages.pages.size - 5).coerceAtLeast(1))) {
                            requestPreloadChapter(nextChapter)
                        }
                    } else {
                        val previousChapter = previousChapter.value
                        val nextChapter = nextChapter.value
                        if (page.chapter == previousChapter) {
                            viewerChapters.value = viewerChapters.value.movePrev()
                            initChapters(params.mangaId, page.chapter.chapter.id, fromMenuButton = false)
                        } else if (page.chapter == nextChapter) {
                            viewerChapters.value = viewerChapters.value.moveNext()
                            initChapters(params.mangaId, page.chapter.chapter.id, fromMenuButton = false)
                        }
                    }
                }
        }
    }

    fun navigate(navigationRegion: Navigation): Boolean {
        scope.launch {
            val moveTo = when (navigationRegion) {
                Navigation.MENU -> {
                    setReaderSettingsMenuOpen(!readerSettingsMenuOpen.value)
                    null
                }

                Navigation.NEXT -> MoveTo.Next

                Navigation.PREV -> MoveTo.Previous

                Navigation.RIGHT -> when (readerModeSettings.direction.value) {
                    Direction.Left -> MoveTo.Previous
                    else -> MoveTo.Next
                }

                Navigation.LEFT -> when (readerModeSettings.direction.value) {
                    Direction.Left -> MoveTo.Next
                    else -> MoveTo.Previous
                }

                Navigation.DOWN -> when (readerModeSettings.direction.value) {
                    Direction.Up -> MoveTo.Previous
                    else -> MoveTo.Next
                }

                Navigation.UP -> when (readerModeSettings.direction.value) {
                    Direction.Up -> MoveTo.Next
                    else -> MoveTo.Previous
                }
            }
            if (moveTo != null) {
                _pageEmitter.emit(PageMove.Direction(moveTo))
            }
        }
        return true
    }

    fun navigate(page: Int) {
        log.info { "Navigate to $page" }
        scope.launch {
            _pageEmitter.emit(PageMove.Page(pages.value.getOrNull(page) ?: return@launch))
        }
    }

    fun progress(page: ReaderItem) {
        log.info { "Progressed to $page" }
        _currentPage.value = page
    }

    fun retry(page: ReaderPage) {
        log.info { "Retrying ${page.index2}" }
        chapter.value?.pageLoader?.retryPage(page)
    }

    fun setMangaReaderMode(mode: String) {
        val label = readerModeResource(mode)?.toPlatformString()
            ?: if (mode == MangaMeta.DEFAULT_READER_MODE) MR.strings.default_reader_mode.toPlatformString() else mode
        toast(MR.strings.reader_mode_changed.toPlatformString(label))
        scope.launchDefault {
            manga.value?.let {
                updateMangaMeta.await(it, mode, onError = { toast(it.toReaderFailure().message.toPlatformString()) })
            }
            initManga(params.mangaId)
        }
    }

    fun setReaderSettingsMenuOpen(open: Boolean) {
        _readerSettingsMenuOpen.value = open
    }

    fun prevChapter() {
        scope.launchDefault {
            val prevChapter = previousChapter.value ?: return@launchDefault
            try {
                _state.value = ReaderChapter.State.Wait
                chapter.value?.pageLoader?.cancelPending()
                sendProgress()
                viewerChapters.value = viewerChapters.value.movePrev()
                initChapters(params.mangaId, prevChapter.chapter.id, fromMenuButton = true)
            } catch (e: Exception) {
                e.throwIfCancellation()
                log.warn(e) { "Error loading prev chapter" }
                _state.value = ReaderChapter.State.Error(e)
            }
        }
    }

    fun nextChapter() {
        scope.launchDefault {
            val nextChapter = nextChapter.value ?: return@launchDefault
            try {
                _state.value = ReaderChapter.State.Wait
                chapter.value?.pageLoader?.cancelPending()
                sendProgress()
                viewerChapters.value = viewerChapters.value.moveNext()
                initChapters(params.mangaId, nextChapter.chapter.id, fromMenuButton = true)
            } catch (e: Exception) {
                e.throwIfCancellation()
                log.warn(e) { "Error loading next chapter" }
                _state.value = ReaderChapter.State.Error(e)
            }
        }
    }

    private suspend fun initManga(mangaId: Long) {
        getManga.asFlow(mangaId)
            .take(1)
            .onEach {
                manga.value = it
            }
            .catch { log.warn(it) { "Error loading manga" } }
            .collect()
        mangaLoaded.complete(Unit)
    }

    // the manga carries the hidden scanlators, so neighbours wait briefly for it
    private suspend fun chapterOptions(): ChapterListOptions? {
        withTimeoutOrNull(OFFLINE_FALLBACK_MS) { mangaLoaded.await() }
        return manga.value?.meta?.chapterOptions
    }

    private suspend fun fetchChapters(
        mangaId: Long,
        chapterId: Long,
    ): List<Chapter>? {
        val offline = offlineChapters.store.isDownloaded(mangaId, chapterId)
        var failure: Throwable? = null
        val fetch = suspend {
            getChapters.asFlow(mangaId)
                .take(1)
                .catch {
                    failure = it
                    log.warn(it) { "Error getting chapters for $mangaId" }
                }
                .singleOrNull()
        }
        val remote = if (offline) withTimeoutOrNull(OFFLINE_FALLBACK_MS) { fetch() } else fetch()
        if (remote != null) return progressSync.withPending(remote)
        if (offline) return progressSync.withPending(offlineChapters.store.chaptersOf(mangaId))
        _state.value = ReaderChapter.State.Error(failure ?: Exception("No chapters found"))
        return null
    }

    private suspend fun initChapters(
        mangaId: Long,
        chapterId: Long,
        fromMenuButton: Boolean = true,
    ) {
        log.debug { "Loading chapter index $chapterId" }
        val (chapter, pages) = coroutineScope {
            val chapters = fetchChapters(mangaId, chapterId)
                ?: return@coroutineScope null
            val chapter = chapters.find { it.id == chapterId }
                ?.let { ReaderChapter(it) }
                ?: run {
                    _state.value = ReaderChapter.State.Error(Exception("Chapter $chapterId not found"))
                    return@coroutineScope null
                }
            val pages = loader.loadChapter(chapter)
            viewerChapters.update { it.copy(currChapter = chapter) }

            val options = chapterOptions()
            if (viewerChapters.value.nextChapter == null) {
                val nextChapter = chapters.nextReadable(chapter.chapter, options)
                if (nextChapter != null) {
                    val nextReaderChapter = ReaderChapter(nextChapter)
                    viewerChapters.update { it.copy(nextChapter = nextReaderChapter) }
                } else {
                    viewerChapters.update { it.copy(nextChapter = null) }
                }
            }

            if (viewerChapters.value.prevChapter == null) {
                val prevChapter = chapters.previousReadable(chapter.chapter, options)
                if (prevChapter != null) {
                    val prevReaderChapter = ReaderChapter(prevChapter)
                    viewerChapters.update { it.copy(prevChapter = prevReaderChapter) }
                } else {
                    viewerChapters.update { it.copy(prevChapter = null) }
                }
            }
            chapter to pages
        } ?: return

        if (fromMenuButton) {
            chapter.stateObserver
                .onEach {
                    _state.value = it
                }
                .launchIn(chapter.scope)

            pages
                .filterIsInstance<PagesState.Success>()
                .onEach { (pageList) ->
                    val resumeIndex = resumePageIndex(chapter.chapter.lastPageRead, pageList.size)
                    val lastPageReadOffset = chapter.chapter.meta.bunkoPageOffset
                    if (resumeIndex > 0 && lastPageReadOffset != 0) {
                        _currentPageOffset.value = lastPageReadOffset
                    }
                    _currentPage.value = pageList[resumeIndex]
                        .also { chapter.pageLoader?.loadPage(it) }
                }
                .launchIn(chapter.scope)
        }
    }

    fun requestPreloadChapter(chapter: ReaderChapter) {
        if (chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error) {
            return
        }
        log.debug { "Preloading ${chapter.chapter.index}" }
        val pages = loader.loadChapter(chapter)
        pages
            .filterIsInstance<PagesState.Success>()
            .take(1)
            .onEach { chapter.pageLoader?.preloadFirstPages(NEXT_CHAPTER_PRELOAD) }
            .launchIn(chapter.scope)
    }

    private fun markChapterRead(chapter: ReaderChapter) = progressSync.markRead(chapter.chapter)

    fun sendProgress(
        chapter: Chapter? = this.chapter.value?.chapter,
        lastPageRead: Int = (currentPage.value as? ReaderPage)?.index2 ?: 0,
    ) {
        chapter ?: return
        progressSync.lastPageRead(chapter, lastPageRead)
    }

    val imageSaveTarget = readerPreferences.imageSaveTarget().stateIn(scope)

    fun savePage(page: ReaderPage, target: ImageSaveTarget, remember: Boolean = false) {
        if (remember) readerPreferences.imageSaveTarget().set(target)
        scope.launch {
            val message = if (target == ImageSaveTarget.Server) saveToServer(page) else saveToPhone(page)
            message?.let { toast(it.toPlatformString()) }
        }
    }

    private suspend fun saveToPhone(page: ReaderPage): StringResource {
        val chapter = page.chapter.chapter
        val saved = page.chapter.pageLoader?.readPage(page) { source ->
            val extension = imageExtension(ByteArray(IMAGE_HEAD).also { source.peek().read(it) })
            val title = mangaTitle.value
            imageStore.save(folderName(title), pageFileName(title, chapter.name, page.index2 + 1, extension), imageMime(extension)) { it.writeAll(source) }
        }?.onFailure { log.warn(it) { "Failed to save page ${page.index2} of chapter ${chapter.id}" } }
        return if (saved?.isSuccess == true) MR.strings.reader_image_saved else MR.strings.reader_image_save_failed
    }

    private suspend fun saveToServer(page: ReaderPage): StringResource? {
        var failed = false
        queueChapterDownload.await(page.chapter.chapter.id, onError = { failed = true; toast(it.toReaderFailure().message.toPlatformString()) })
        return if (failed) null else MR.strings.reader_chapter_queued
    }

    fun updateLastPageReadOffset(offset: Int) {
        progressSync.pageOffset(chapter.value?.chapter ?: return, offset)
    }

    override fun onDispose() {
        discordPresence.setReading(null)
        viewerChapters.value.recycle()
        scope.cancel()
    }

    data class Params(
        val chapterId: Long,
        val mangaId: Long,
    )

    private companion object {
        private const val IMAGE_HEAD = 12
        private const val NEXT_CHAPTER_PRELOAD = 3
        private const val OFFLINE_FALLBACK_MS = 2_500L
        private val log = logging()
    }
}
