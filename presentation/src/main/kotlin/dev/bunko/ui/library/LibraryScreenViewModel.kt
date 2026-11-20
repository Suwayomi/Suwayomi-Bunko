/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library

import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import dev.bunko.ui.base.offline.OfflineChapters
import dev.bunko.ui.base.userMessage
import dev.bunko.ui.manga.accent.CoverAccents
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import dev.bunko.core.prefs.getAsFlow
import dev.bunko.domain.category.interactor.GetCategories
import dev.bunko.domain.category.interactor.GetMangaListFromCategory
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.chapter.interactor.DeleteChapterDownload
import dev.bunko.domain.chapter.interactor.GetChapters
import dev.bunko.domain.library.interactor.RemoveMangaFromLibrary
import dev.bunko.domain.library.model.FilterState
import dev.bunko.domain.library.model.Sort
import dev.bunko.domain.library.service.LibraryPreferences
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.manga.model.MangaStatus
import dev.bunko.domain.updates.interactor.UpdateCategory
import dev.bunko.domain.updates.interactor.UpdateLibrary
import dev.bunko.i18n.MR
import androidx.lifecycle.SavedStateHandle
import dev.bunko.ui.base.state.getStateFlow
import dev.bunko.ui.util.lang.CollatorComparator
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Stable
sealed class LibraryState {
    @Stable
    data object Loading : LibraryState()

    @Stable
    data object Empty : LibraryState()

    @Stable
    data class Failed(
        val e: Throwable,
    ) : LibraryState()

    @Stable
    data class Loaded(
        val categories: ImmutableList<Category>,
    ) : LibraryState()
}

@Stable
sealed class CategoryState {
    @Stable
    data object Loading : CategoryState()

    @Stable
    data class Failed(
        val e: Throwable,
    ) : CategoryState()

    @Stable
    data class Loaded(
        val items: StateFlow<ImmutableList<Manga>>,
        val unfilteredItems: MutableStateFlow<ImmutableList<Manga>>,
    ) : CategoryState()
}

private typealias LibraryMap = MutableMap<Long, MutableStateFlow<CategoryState>>

private data class Library(
    val categories: MutableStateFlow<LibraryState>,
    val mangaMap: LibraryMap,
)

private fun LibraryMap.getManga(id: Long) =
    getOrPut(id) {
        MutableStateFlow(CategoryState.Loading)
    }

private fun LibraryMap.setError(
    id: Long,
    e: Throwable,
) {
    getManga(id).value = CategoryState.Failed(e)
}

private fun LibraryMap.setManga(
    id: Long,
    manga: ImmutableList<Manga>,
    getItemsFlow: (StateFlow<List<Manga>>) -> StateFlow<ImmutableList<Manga>>,
) {
    val flow = getManga(id)
    when (val state = flow.value) {
        is CategoryState.Loaded -> state.unfilteredItems.value = manga

        else -> {
            val unfilteredItems = MutableStateFlow(manga)
            flow.value = CategoryState.Loaded(getItemsFlow(unfilteredItems), unfilteredItems)
        }
    }
}

@Inject
class LibraryScreenViewModel(
    private val getCategories: GetCategories,
    private val getMangaListFromCategory: GetMangaListFromCategory,
    private val removeMangaFromLibrary: RemoveMangaFromLibrary,
    private val getChapters: GetChapters,
    private val deleteChapterDownload: DeleteChapterDownload,
    private val updateLibrary: UpdateLibrary,
    private val updateCategory: UpdateCategory,
    private val offlineChapters: OfflineChapters,
    private val coverAccents: CoverAccents,
    private val batchActions: LibraryBatchActions,
    libraryResume: LibraryResume,
    libraryPreferences: LibraryPreferences,
    contextWrapper: ContextWrapper,
    @Assisted private val savedStateHandle: SavedStateHandle,
) : ViewModel(contextWrapper) {
    private val library = Library(MutableStateFlow(LibraryState.Loading), mutableMapOf())
    val categories = library.categories.asStateFlow()

    private val _selectedCategoryIndex by savedStateHandle.getStateFlow { 0 }
    val selectedCategoryIndex = _selectedCategoryIndex.asStateFlow()

    private val _showingMenu by savedStateHandle.getStateFlow { false }
    val showingMenu = _showingMenu.asStateFlow()

    val displayMode = libraryPreferences.displayMode().stateIn(scope)
    val gridColumns = libraryPreferences.gridColumns().stateIn(scope)
    val gridColumnsLandscape = libraryPreferences.gridColumnsLandscape().stateIn(scope)
    val gridSize = libraryPreferences.gridSize().stateIn(scope)
    val unreadBadges = libraryPreferences.unreadBadge().stateIn(scope)
    val downloadBadges = libraryPreferences.downloadBadge().stateIn(scope)
    val languageBadges = libraryPreferences.languageBadge().stateIn(scope)
    val localBadges = libraryPreferences.localBadge().stateIn(scope)

    val resume = libraryResume.entry.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    private val sortMode = libraryPreferences.sortMode().stateIn(scope)
    private val sortAscending = libraryPreferences.sortAscending().stateIn(scope)

    private val filter: Flow<(Manga) -> Boolean> = combine(
        libraryPreferences.filterDownloaded().getAsFlow(),
        libraryPreferences.filterUnread().getAsFlow(),
        libraryPreferences.filterCompleted().getAsFlow(),
    ) { downloaded, unread, completed ->
        { manga ->
            when (downloaded) {
                FilterState.EXCLUDED -> manga.downloadCount == null || manga.downloadCount == 0
                FilterState.INCLUDED -> manga.downloadCount != null && (manga.downloadCount ?: 0) > 0
                FilterState.IGNORED -> true
            } &&
                when (unread) {
                    FilterState.EXCLUDED -> manga.unreadCount == null || manga.unreadCount == 0
                    FilterState.INCLUDED -> manga.unreadCount != null && (manga.unreadCount ?: 0) > 0
                    FilterState.IGNORED -> true
                } &&
                when (completed) {
                    FilterState.EXCLUDED -> manga.status != MangaStatus.COMPLETED
                    FilterState.INCLUDED -> manga.status == MangaStatus.COMPLETED
                    FilterState.IGNORED -> true
                }
        }
    }

    private val _onDeviceOnly by savedStateHandle.getStateFlow { false }
    val onDeviceOnly = _onDeviceOnly.asStateFlow()

    private val onDeviceIds: Flow<Set<Long>?> = combine(_onDeviceOnly, offlineChapters.states) { on, _ -> on }
        .mapLatest { on ->
            if (on) withContext(Dispatchers.IO) { offlineChapters.store.all().map { it.chapter.mangaId }.toSet() } else null
        }

    private val _query by savedStateHandle.getStateFlow { "" }
    val query = _query.asStateFlow()

    private val comparator = combine(sortMode, sortAscending) { sortMode, sortAscending ->
        getComparator(sortMode, sortAscending)
    }.stateIn(scope, SharingStarted.Eagerly, compareBy { it.title })

    init {
        getLibrary()
    }

    private val categoryJobs = mutableMapOf<Long, Job>()

    private fun getLibrary() {
        library.categories.value = LibraryState.Loading
        getCategories.asFlow()
            .onEach { categories ->
                if (categories.isEmpty()) {
                    library.categories.value = LibraryState.Empty
                    return@onEach
                }
                library.categories.value = LibraryState.Loaded(
                    categories.sortedBy { it.order }
                        .toImmutableList(),
                )
                val ids = categories.mapTo(HashSet()) { it.id }
                categoryJobs.keys.filterNot { it in ids }.forEach { categoryJobs.remove(it)?.cancel() }
                categories.filterNot { it.id in categoryJobs }.forEach { category ->
                    categoryJobs[category.id] = watchCategory(category)
                }
            }
            .catch {
                library.categories.value = LibraryState.Failed(it)
                log.warn(it) { "Failed to get categories" }
            }
            .launchIn(scope)
    }

    private fun watchCategory(category: Category): Job =
        getMangaListFromCategory.asFlow(category, holdsManga = { ids -> ids.any { it in shownIds(category.id) } })
            .onEach {
                library.mangaMap.setManga(
                    id = category.id,
                    manga = it.toImmutableList(),
                    getItemsFlow = ::getMangaItemsFlow,
                )
                syncCount(category.id, it.size)
                coverAccents.precache(it)
            }
            .catch {
                log.warn(it) { "Failed to get manga list from category ${category.name}" }
                library.mangaMap.setError(category.id, it)
            }
            .launchIn(scope)

    private fun syncCount(
        categoryId: Long,
        size: Int,
    ) {
        val state = library.categories.value as? LibraryState.Loaded ?: return
        if (state.categories.none { it.id == categoryId && it.size != size }) return
        library.categories.value = LibraryState.Loaded(
            state.categories.map { if (it.id == categoryId) it.copy(size = size) else it }.toImmutableList(),
        )
    }

    private fun shownIds(categoryId: Long): List<Long> =
        (library.mangaMap[categoryId]?.value as? CategoryState.Loaded)?.unfilteredItems?.value?.map { it.id }.orEmpty()

    fun setSelectedPage(page: Int) {
        _selectedCategoryIndex.value = page
    }

    fun setShowingMenu(showingMenu: Boolean) {
        _showingMenu.value = showingMenu
    }

    private fun getComparator(
        sortMode: Sort,
        ascending: Boolean,
    ): Comparator<Manga> {
        val sortFn: (Manga, Manga) -> Int = when (sortMode) {
            Sort.ALPHABETICAL -> {
                val locale = Locale.current
                val collator = CollatorComparator(locale);

                { a, b ->
                    collator.compare(a.title.toLowerCase(locale), b.title.toLowerCase(locale))
                }
            }

            Sort.UNREAD -> {
                { a, b ->
                    when {
                        (a.unreadCount ?: 0) == (b.unreadCount ?: 0) -> 0

                        a.unreadCount == null || a.unreadCount == 0 -> if (ascending) 1 else -1

                        b.unreadCount == null || b.unreadCount == 0 -> if (ascending) -1 else 1

                        else -> (a.unreadCount ?: 0).compareTo(b.unreadCount ?: 0)
                    }
                }
            }

            Sort.DATE_ADDED -> {
                { a, b ->
                    a.inLibraryAt.compareTo(b.inLibraryAt)
                }
            }
        }
        return if (ascending) {
            Comparator(sortFn)
        } else {
            Comparator(sortFn).reversed()
        }
    }

    private suspend fun filterManga(
        query: String,
        mangaList: List<Manga>,
    ): List<Manga> {
        if (query.isBlank()) return mangaList
        val queries = query.split(" ")
        return mangaList.asFlow()
            .filter { manga ->
                queries.all { query ->
                    manga.title.contains(query, true) ||
                        manga.author.orEmpty().contains(query, true) ||
                        manga.artist.orEmpty().contains(query, true) ||
                        manga.genre.any { it.contains(query, true) } ||
                        manga.description.orEmpty().contains(query, true) ||
                        manga.status.name.contains(query, true)
                }
            }
            .cancellable()
            .buffer()
            .toList()
    }

    private fun getMangaItemsFlow(unfilteredItemsFlow: StateFlow<List<Manga>>): StateFlow<ImmutableList<Manga>> =
        combine(
            unfilteredItemsFlow,
            query,
        ) {
                unfilteredItems,
                query,
            ->
            filterManga(query, unfilteredItems)
        }.combine(filter) { filteredManga, filterer ->
            filteredManga.filter(filterer)
        }.combine(onDeviceIds) { filteredManga, ids ->
            if (ids == null) filteredManga else filteredManga.filter { it.id in ids }
        }.combine(comparator) { filteredManga, comparator ->
            filteredManga.sortedWith(comparator)
        }.map {
            it.toImmutableList()
        }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    fun getLibraryForCategoryId(id: Long): StateFlow<CategoryState> = library.mangaMap.getManga(id)

    private fun shownSnapshot() =
        library.mangaMap.mapValues { (_, state) -> (state.value as? CategoryState.Loaded)?.unfilteredItems?.value }

    private fun restore(before: Map<Long, ImmutableList<Manga>?>) =
        before.forEach { (id, items) -> if (items != null) setShown(id, items) }

    fun removeManga(mangaIds: Set<Long>) {
        if (mangaIds.isEmpty()) return
        val before = shownSnapshot()
        before.forEach { (id, items) ->
            if (items != null) setShown(id, items.filterNot { it.id in mangaIds }.toImmutableList())
        }
        scope.launch {
            val removed = removeMangaFromLibrary.await(mangaIds.toList(), onError = { toast(it.userMessage.toPlatformString()) })
            if (!removed) restore(before)
        }
    }

    fun deleteDownloads(mangaIds: Set<Long>) {
        if (mangaIds.isEmpty()) return
        val before = shownSnapshot()
        before.forEach { (id, items) ->
            if (items != null) {
                setShown(id, items.map { if (it.id in mangaIds) it.copy(downloadCount = 0) else it }.toImmutableList())
            }
        }
        scope.launch {
            var failed = false
            val unlisted = mutableSetOf<Long>()
            val downloaded = mangaIds.flatMap { id ->
                getChapters.await(id, onError = { unlisted += id }) ?: emptyList()
            }.filter { it.downloaded }
            if (unlisted.isNotEmpty()) toast(MR.strings.library_delete_downloads_incomplete.toPlatformString())
            if (downloaded.isNotEmpty()) {
                deleteChapterDownload.await(
                    downloaded.map { it.id },
                    downloaded.map { it.mangaId }.distinct(),
                    onError = {
                        failed = true
                        toast(it.userMessage.toPlatformString())
                    },
                )
            }
            val offline = withContext(Dispatchers.IO) { offlineChapters.store.all().map { it.chapter } }
                .filter { it.mangaId in mangaIds }
            offlineChapters.delete(offline)
            if (failed) restore(before) else restoreBadges(before, unlisted)
        }
    }

    private fun restoreBadges(
        before: Map<Long, ImmutableList<Manga>?>,
        mangaIds: Set<Long>,
    ) {
        if (mangaIds.isEmpty()) return
        before.forEach { (id, items) ->
            val original = items?.filter { it.id in mangaIds }?.associateBy { it.id } ?: return@forEach
            val shown = shownIds(id).toSet()
            val current = (library.mangaMap[id]?.value as? CategoryState.Loaded)?.unfilteredItems?.value ?: return@forEach
            if (shown.none { it in original }) return@forEach
            setShown(id, current.map { original[it.id] ?: it }.toImmutableList())
        }
    }

    private fun setShown(
        categoryId: Long,
        items: ImmutableList<Manga>,
    ) {
        (library.mangaMap[categoryId]?.value as? CategoryState.Loaded)?.unfilteredItems?.value = items
        syncCount(categoryId, items.size)
    }

    fun toggleOnDeviceOnly() {
        _onDeviceOnly.value = !_onDeviceOnly.value
    }

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun updateLibrary() {
        scope.launch { updateLibrary.await(onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun updateCategory(category: Category) {
        scope.launch { updateCategory.await(category, onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val editableCategories = getCategories.asFlow(true)
        .map { it.toImmutableList() }
        .catch { log.warn(it) { "Failed to get categories" } }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    /** Starts a server update of the shown category; the spinner only covers queueing it. */
    fun refreshCategory(index: Int) {
        val category = (categories.value as? LibraryState.Loaded)?.categories?.getOrNull(index) ?: return
        scope.launch {
            _isRefreshing.value = true
            updateCategory.await(category, onError = { toast(it.userMessage.toPlatformString()) })
            _isRefreshing.value = false
        }
    }

    fun setRead(
        mangaIds: Set<Long>,
        read: Boolean,
    ) {
        scope.launch { batchActions.setRead(mangaIds, read, onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun downloadUnread(mangaIds: Set<Long>) {
        scope.launch { batchActions.downloadUnread(mangaIds, onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun setCategories(
        mangaIds: Set<Long>,
        categories: List<Category>,
    ) {
        scope.launch { batchActions.setCategories(mangaIds, categories, onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    private companion object {
        private val log = logging()
    }
}
