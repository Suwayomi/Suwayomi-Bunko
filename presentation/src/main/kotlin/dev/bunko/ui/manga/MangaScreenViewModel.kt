/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga

import dev.bunko.ui.base.userMessage
import dev.bunko.core.util.DateHandler
import dev.bunko.domain.category.interactor.AddMangaToCategory
import dev.bunko.domain.category.interactor.GetCategories
import dev.bunko.domain.category.interactor.GetMangaCategories
import dev.bunko.domain.category.interactor.RemoveMangaFromCategory
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.chapter.interactor.DeleteChapterDownload
import dev.bunko.domain.chapter.interactor.GetChapters
import dev.bunko.domain.chapter.interactor.RefreshChapters
import dev.bunko.domain.chapter.interactor.UpdateChapter
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.model.ChapterDisplay
import dev.bunko.domain.chapter.model.ChapterListOptions
import dev.bunko.domain.chapter.model.ChapterSort
import dev.bunko.domain.chapter.model.applyChapterOptions
import dev.bunko.domain.chapter.model.missingChapterCount
import dev.bunko.domain.chapter.model.scanlators
import dev.bunko.domain.chapter.model.withoutExcludedScanlators
import dev.bunko.domain.library.model.FilterState
import dev.bunko.domain.download.interactor.BatchChapterDownload
import dev.bunko.domain.download.interactor.QueueChapterDownload
import dev.bunko.domain.download.interactor.StopChapterDownload
import dev.bunko.domain.download.service.DownloadService
import dev.bunko.domain.library.interactor.AddMangaToLibrary
import dev.bunko.domain.library.interactor.RemoveMangaFromLibrary
import dev.bunko.domain.library.service.LibraryPreferences
import dev.bunko.domain.manga.interactor.GetManga
import dev.bunko.domain.manga.interactor.RefreshManga
import dev.bunko.domain.manga.interactor.SetChapterListOptions
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.track.interactor.Tracking
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.ui.base.chapter.ChapterDownloadItem
import dev.bunko.ui.base.chapter.ChapterDownloadState
import dev.bunko.ui.base.model.StableHolder
import dev.bunko.ui.base.offline.OfflineChapters
import dev.bunko.ui.base.offline.OfflineState
import dev.bunko.ui.manga.tracking.MangaTracking
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.bunko.ui.manga.accent.CoverAccents
import dev.bunko.ui.reader.ReaderProgressSync
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class MangaScreenViewModel(
    private val dateHandler: DateHandler,
    private val getManga: GetManga,
    private val refreshManga: RefreshManga,
    private val getChapters: GetChapters,
    private val refreshChapters: RefreshChapters,
    private val updateChapter: UpdateChapter,
    private val progressSync: ReaderProgressSync,
    private val queueChapterDownload: QueueChapterDownload,
    private val stopChapterDownload: StopChapterDownload,
    private val deleteChapterDownload: DeleteChapterDownload,
    private val getCategories: GetCategories,
    private val getMangaCategories: GetMangaCategories,
    private val addMangaToCategory: AddMangaToCategory,
    private val removeMangaFromCategory: RemoveMangaFromCategory,
    private val addMangaToLibrary: AddMangaToLibrary,
    private val removeMangaFromLibrary: RemoveMangaFromLibrary,
    private val batchChapterDownload: BatchChapterDownload,
    private val offlineChapters: OfflineChapters,
    private val setChapterListOptions: SetChapterListOptions,
    val coverAccents: CoverAccents,
    trackingInteractor: Tracking,
    serverPreferences: ServerPreferences,
    uiPreferences: UiPreferences,
    libraryPreferences: LibraryPreferences,
    contextWrapper: ContextWrapper,
    @Assisted private val params: Params,
) : ViewModel(contextWrapper) {
    private val _manga = MutableStateFlow<Manga?>(null)
    val manga = _manga.asStateFlow()

    private val _chapters = MutableStateFlow<ImmutableList<ChapterDownloadItem>>(persistentListOf())
    val chapters = _chapters.asStateFlow()

    val offlineStates = offlineChapters.states

    private val chapterDefaults = libraryPreferences.chapterListDefaults()
    private val localOptions = MutableStateFlow<LocalOptions>(LocalOptions.Stored)

    val chapterOptions = combine(manga, localOptions, chapterDefaults.changes()) { manga, local, default ->
        when (local) {
            LocalOptions.Stored -> manga?.meta?.chapterOptions ?: default
            LocalOptions.FollowDefault -> default
            is LocalOptions.Custom -> local.options
        }
    }.stateIn(scope, SharingStarted.Eagerly, chapterDefaults.get())

    val chapterOptionsCustomised = combine(chapterOptions, chapterDefaults.changes()) { options, default -> options != default }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val displayChapters = combine(chapters, chapterOptions, offlineStates) { chapters, options, offline ->
        chapters.applyChapterOptions(options, { it.chapter }) { offline[it.id] == OfflineState.Downloaded }
            .toImmutableList()
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    val scanlators = chapters.map { items -> items.map { it.chapter }.scanlators().toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    // counted on the groups being read, other filters would open fake gaps
    val missingChapterCount = combine(chapters, chapterOptions) { chapters, options ->
        chapters.map { it.chapter }.withoutExcludedScanlators(options).missingChapterCount()
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    val resumeChapter = combine(chapters, chapterOptions) { chapters, options ->
        chapters.withoutExcludedScanlators(options) { it.chapter }
            .filter { !it.chapter.read }
            .minByOrNull { it.chapter.index }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val tracking = MangaTracking(
        scope = scope,
        tracking = trackingInteractor,
        mangaId = params.mangaId,
        serverUrl = { serverPreferences.serverUrl().get().toString() },
        onError = { toast(it.userMessage.toPlatformString()) },
    )

    private val selectedIds = MutableStateFlow<ImmutableList<Long>>(persistentListOf())
    private var lastSelectedId: Long? = null
    val selectedItems = combine(chapters, selectedIds) { chapters, selecteditems ->
        chapters.filter { it.isSelected(selecteditems) }.toImmutableList()
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    private val loadingManga = MutableStateFlow(true)
    private val loadingChapters = MutableStateFlow(true)
    private val refreshingChapters = MutableStateFlow(false)
    private val refreshingManga = MutableStateFlow(false)
    val isLoading = combine(loadingManga, loadingChapters) { manga, chapters -> manga || chapters }
        .stateIn(scope, SharingStarted.Eagerly, true)
    val isRefreshing = combine(refreshingManga, refreshingChapters) { manga, chapters -> manga || chapters }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val categories = getCategories.asFlow(true)
        .map { it.toImmutableList() }
        .catch {
            toast(it.userMessage.toPlatformString())
            log.warn(it) { "Failed to get categories" }
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    private val _mangaCategories = MutableStateFlow<ImmutableList<Category>>(persistentListOf())
    val mangaCategories = _mangaCategories.asStateFlow()

    val categoriesExist = categories.map { it.isNotEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, true)

    val inActionMode = selectedIds.map { it.isNotEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val chooseCategoriesFlow = MutableSharedFlow<Unit>()
    val chooseCategoriesFlowHolder = StableHolder(chooseCategoriesFlow.asSharedFlow())

    private val reloadManga = MutableSharedFlow<Unit>()
    private val reloadChapters = MutableSharedFlow<Unit>()

    val dateTimeFormatter = uiPreferences.dateFormat().changes()
        .map {
            dateHandler.getDateFormat(it)
        }
        .stateIn(scope, SharingStarted.Eagerly, dateHandler.getDateFormat(uiPreferences.dateFormat().get()))

    init {
        tracking.load()

        localOptions
            .drop(1)
            .filterNot { it == LocalOptions.Stored }
            .debounce(OPTIONS_SAVE_DELAY_MS)
            .onEach { local ->
                setChapterListOptions.await(
                    params.mangaId,
                    (local as? LocalOptions.Custom)?.options,
                    onError = { toast(it.userMessage.toPlatformString()) },
                )
            }
            .launchIn(scope)

        DownloadService.registerWatch(params.mangaId)
            .mapLatest { downloadingChapters ->
                chapters.value.forEach { chapter ->
                    chapter.updateFrom(downloadingChapters)
                }
            }
            .launchIn(scope)

        var firstMangaLoad = true
        reloadManga.onStart { emit(Unit) }.flatMapLatest {
            loadingManga.value = true
            getManga.asFlow(params.mangaId, cacheFirst = firstMangaLoad).also { firstMangaLoad = false }
        }
            .onEach {
                _manga.value = it
                loadingManga.value = false
            }
            .catch {
                toast(it.userMessage.toPlatformString())
                log.warn(it) { "Error when loading manga" }
                loadingManga.value = false
            }
            .launchIn(scope)

        reloadChapters.onStart { emit(Unit) }.flatMapLatest {
            loadingChapters.value = true
            getChapters.asFlow(params.mangaId)
        }
            .map { progressSync.withPending(it) }
            .onEach {
                updateChapters(it)
                loadingChapters.value = false
            }
            .catch {
                toast(it.userMessage.toPlatformString())
                log.warn(it) { "Error when getting chapters" }
                loadingChapters.value = false
            }
            .launchIn(scope)

        scope.launch {
            val mangaCategories = getMangaCategories.await(params.mangaId, onError = { toast(it.userMessage.toPlatformString()) })
            if (mangaCategories != null) {
                _mangaCategories.value = mangaCategories.toImmutableList()
            }
        }

        scope.launch {
            val manga = manga.first { it != null }!!
            if (!manga.initialized) {
                refreshManga()
            }
        }
    }

    fun loadManga() {
        scope.launch {
            reloadManga.emit(Unit)
        }
    }

    fun loadChapters() {
        scope.launch {
            reloadChapters.emit(Unit)
        }
    }

    fun updateChapters(chapters: List<Chapter>) {
        _chapters.value = chapters.sortedByDescending { it.index }.toDownloadChapters()
    }

    fun refreshManga() {
        scope.launch {
            refreshingManga.value = true
            val manga = refreshManga.await(
                params.mangaId,
                onError = {
                    log.warn(it) { "Error when refreshing manga" }
                    toast(it.userMessage.toPlatformString())
                },
            )
            if (manga != null) {
                _manga.value = manga
            }
            refreshingManga.value = false
        }
        scope.launch {
            refreshingChapters.value = true
            val chapters = refreshChapters.await(
                params.mangaId,
                onError = {
                    log.warn(it) { "Error when refreshing chapters" }
                    toast(it.userMessage.toPlatformString())
                },
            )
            if (!chapters.isNullOrEmpty()) {
                updateChapters(progressSync.withPending(chapters))
            }
            refreshingChapters.value = false
        }
    }

    fun setCategories() {
        scope.launch {
            manga.value ?: return@launch
            chooseCategoriesFlow.emit(Unit)
        }
    }

    fun toggleFavorite() {
        scope.launch {
            manga.value?.let { manga ->
                if (manga.inLibrary) {
                    _manga.value = manga.copy(inLibrary = false)
                    val removed = removeMangaFromLibrary.await(listOf(manga.id), onError = { toast(it.userMessage.toPlatformString()) })
                    if (!removed) _manga.value = manga
                } else {
                    if (categories.value.isEmpty()) {
                        addFavorite(emptyList(), emptyList())
                    } else {
                        chooseCategoriesFlow.emit(Unit)
                    }
                }
            }
        }
    }

    fun addFavorite(
        categories: List<Category>,
        oldCategories: List<Category>,
    ) {
        scope.launch {
            manga.value?.let { manga ->
                if (manga.inLibrary) {
                    if (oldCategories.isEmpty()) {
                        removeMangaFromCategory.await(manga.id, 0, onError = { toast(it.userMessage.toPlatformString()) })
                    } else {
                        oldCategories.filterNot { it in categories }.forEach {
                            removeMangaFromCategory.await(manga, it, onError = { toast(it.userMessage.toPlatformString()) })
                        }
                    }
                } else {
                    _manga.value = manga.copy(inLibrary = true)
                    val added = addMangaToLibrary.await(manga, onError = { toast(it.userMessage.toPlatformString()) }) != null
                    if (!added) {
                        _manga.value = manga
                        return@launch
                    }
                }
                if (categories.isEmpty()) {
                    addMangaToCategory.await(manga.id, 0, onError = { toast(it.userMessage.toPlatformString()) })
                } else {
                    categories.filterNot { it in oldCategories }.forEach {
                        addMangaToCategory.await(manga, it, onError = { toast(it.userMessage.toPlatformString()) })
                    }
                }

                val mangaCategories = getMangaCategories.await(manga.id, onError = { toast(it.userMessage.toPlatformString()) })
                if (mangaCategories != null) {
                    _mangaCategories.value = mangaCategories.toImmutableList()
                }
            }
        }
    }

    private fun setRead(
        chapterIds: List<Long>,
        read: Boolean,
    ) {
        scope.launch {
            manga.value?.let {
                editChapters(chapterIds) { it.copy(read = read) }
                selectedIds.value = persistentListOf()
                progressSync.remove(chapterIds)
                updateChapter.await(chapterIds, listOf(params.mangaId), read = read, onError = ::resyncChapters)
            }
        }
    }

    fun markRead(id: Long?) = setRead(listOfNotNull(id).ifEmpty { selectedIds.value }, true)

    fun markUnread(id: Long?) = setRead(listOfNotNull(id).ifEmpty { selectedIds.value }, false)

    private fun setBookmarked(
        chapterIds: List<Long>,
        bookmark: Boolean,
    ) {
        scope.launch {
            manga.value?.let {
                editChapters(chapterIds) { it.copy(bookmarked = bookmark) }
                selectedIds.value = persistentListOf()
                updateChapter.await(chapterIds, listOf(params.mangaId), bookmarked = bookmark, onError = ::resyncChapters)
            }
        }
    }

    fun bookmarkChapter(id: Long?) = setBookmarked(listOfNotNull(id).ifEmpty { selectedIds.value }, true)

    fun unBookmarkChapter(id: Long?) = setBookmarked(listOfNotNull(id).ifEmpty { selectedIds.value }, false)

    fun markPreviousRead(index: Int) {
        scope.launch {
            manga.value?.let {
                val chapters = chapters.value
                    .sortedBy { it.chapter.index }
                    .subList(0, index).map { it.chapter.id }
                editChapters(chapters) { it.copy(read = true) }
                selectedIds.value = persistentListOf()
                progressSync.remove(chapters)
                updateChapter.await(chapters, listOf(params.mangaId), read = true, onError = ::resyncChapters)
            }
        }
    }

    private fun editChapters(
        ids: List<Long>,
        change: (Chapter) -> Chapter,
    ) {
        val idSet = ids.toHashSet()
        _chapters.value = _chapters.value.map { item ->
            if (item.chapter.id in idSet) item.withChapter(change(item.chapter)) else item
        }.toImmutableList()
    }

    private suspend fun resyncChapters(error: Throwable) {
        toast(error.userMessage.toPlatformString())
        loadChapters()
    }

    fun downloadChapter(chapterId: Long) {
        scope.launch { queueChapterDownload.await(chapterId, onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun deleteDownload(id: Long?) {
        scope.launch {
            val items = if (id == null) selectedItems.value else listOfNotNull(chapters.value.find { it.chapter.id == id })
            if (items.isEmpty()) return@launch
            items.forEach { it.setDownloaded(false) }
            if (id == null) selectedIds.value = persistentListOf()
            deleteChapterDownload.await(items.map { it.chapter.id }, listOf(params.mangaId), onError = ::resyncChapters)
        }
    }

    fun saveOffline(id: Long?) {
        val picked = if (id == null) selectedItems.value.map { it.chapter } else listOfNotNull(chapters.value.find { it.chapter.id == id }?.chapter)
        manga.value?.let { offlineChapters.download(it, picked) }
        if (id == null) selectedIds.value = persistentListOf()
    }

    fun removeOffline(id: Long?) {
        val picked = if (id == null) selectedItems.value.map { it.chapter } else listOfNotNull(chapters.value.find { it.chapter.id == id }?.chapter)
        offlineChapters.delete(picked)
        if (id == null) selectedIds.value = persistentListOf()
    }

    fun saveUnreadOffline(count: Int?) {
        val fresh = distinctUnread(readableChapters().map { it.chapter }).filter { it.id !in offlineStates.value }
        manga.value?.let { offlineChapters.download(it, if (count == null) fresh else fresh.take(count)) }
    }

    fun stopDownloadingChapter(chapterId: Long) {
        scope.launch {
            chapters.value.find { it.chapter.id == chapterId }
                ?.stopDownloading(stopChapterDownload)
        }
    }

    fun selectAll() {
        scope.launch {
            selectedIds.value = displayChapters.value.map { it.chapter.id }.toImmutableList()
        }
    }

    fun invertSelection() {
        scope.launch {
            selectedIds.value = displayChapters.value.map { it.chapter.id }.minus(selectedIds.value.toSet()).toImmutableList()
        }
    }

    fun selectChapter(id: Long) {
        scope.launch {
            selectedIds.value = selectedIds.value.plus(id).distinct().toImmutableList()
            lastSelectedId = id
        }
    }

    /** A long press while selecting extends the selection from the last picked chapter. */
    fun longPressChapter(id: Long) {
        scope.launch {
            val anchor = lastSelectedId?.takeIf { it in selectedIds.value }
            val visible = displayChapters.value.map { it.chapter.id }
            val from = anchor?.let(visible::indexOf) ?: -1
            val to = visible.indexOf(id)
            selectedIds.value = if (from < 0 || to < 0) {
                selectedIds.value.plus(id)
            } else {
                selectedIds.value.plus(visible.subList(minOf(from, to), maxOf(from, to) + 1))
            }.distinct().toImmutableList()
            lastSelectedId = id
        }
    }

    fun unselectChapter(id: Long) {
        scope.launch {
            selectedIds.value = selectedIds.value.minus(id).toImmutableList()
        }
    }

    fun clearSelection() {
        scope.launch {
            selectedIds.value = persistentListOf()
        }
    }

    fun setUnreadFilter(state: FilterState) = editOptions { it.copy(unread = state) }

    fun setDownloadedFilter(state: FilterState) = editOptions { it.copy(downloaded = state) }

    fun setBookmarkedFilter(state: FilterState) = editOptions { it.copy(bookmarked = state) }

    fun setScanlatorShown(
        scanlator: String,
        shown: Boolean,
    ) = editOptions {
        it.copy(excludedScanlators = if (shown) it.excludedScanlators - scanlator else it.excludedScanlators + scanlator)
    }

    fun showAllScanlators() = editOptions { it.copy(excludedScanlators = emptySet()) }

    fun setSort(sort: ChapterSort) =
        editOptions {
            if (it.sort == sort) it.copy(descending = !it.descending) else it.copy(sort = sort)
        }

    fun setDisplay(display: ChapterDisplay) = editOptions { it.copy(display = display) }

    fun setOptionsAsDefault() {
        chapterDefaults.set(chapterOptions.value.asDefault())
    }

    fun resetOptions() {
        localOptions.value = LocalOptions.FollowDefault
    }

    private fun editOptions(change: (ChapterListOptions) -> ChapterListOptions) {
        localOptions.value = LocalOptions.Custom(change(chapterOptions.value))
    }

    private fun readableChapters() = _chapters.value.withoutExcludedScanlators(chapterOptions.value) { it.chapter }

    fun downloadChapters() {
        scope.launch {
            batchChapterDownload.await(selectedIds.value)
            selectedIds.value = persistentListOf()
        }
    }

    fun downloadNext(next: Int) {
        scope.launch {
            batchChapterDownload.await(
                readableChapters().filter { !it.chapter.read && it.downloadState.value == ChapterDownloadState.NotDownloaded }
                    .map { it.chapter.id }
                    .takeLast(next),
            )
        }
    }

    fun downloadUnread() {
        scope.launch {
            batchChapterDownload.await(
                readableChapters().filter { !it.chapter.read && it.downloadState.value == ChapterDownloadState.NotDownloaded }
                    .map { it.chapter.id },
            )
        }
    }

    fun downloadAll() {
        scope.launch {
            batchChapterDownload.await(
                _chapters.value
                    .filter { it.downloadState.value == ChapterDownloadState.NotDownloaded }
                    .map { it.chapter.id },
            )
        }
    }

    private fun List<Chapter>.toDownloadChapters() =
        map {
            ChapterDownloadItem(null, it)
        }.toImmutableList()

    data class Params(
        val mangaId: Long,
    )

    // edits made on this screen win over the stored meta for the rest of the visit
    private sealed interface LocalOptions {
        data object Stored : LocalOptions

        data object FollowDefault : LocalOptions

        data class Custom(
            val options: ChapterListOptions,
        ) : LocalOptions
    }

    private companion object {
        private val log = logging()
        private const val OPTIONS_SAVE_DELAY_MS = 400L
    }
}
