/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse

import dev.bunko.domain.category.model.Category
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.base.userMessage
import dev.bunko.domain.library.model.DisplayMode
import dev.bunko.domain.library.service.LibraryPreferences
import dev.bunko.domain.source.interactor.GetLatestManga
import dev.bunko.domain.source.interactor.GetMangaPage
import dev.bunko.domain.source.interactor.GetPopularManga
import dev.bunko.domain.source.interactor.GetSearchManga
import dev.bunko.domain.source.interactor.SourcePager
import dev.bunko.domain.source.model.MangaPage
import dev.bunko.domain.source.model.Source
import dev.bunko.domain.source.model.sourcefilters.SourceFilter
import dev.bunko.domain.source.service.CatalogPreferences
import androidx.lifecycle.SavedStateHandle
import dev.bunko.ui.base.state.getStateFlow
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

class SourceScreenViewModel(
    private val source: Source,
    private val getLatestManga: GetLatestManga,
    private val getPopularManga: GetPopularManga,
    private val getSearchManga: GetSearchManga,
    private val catalogPreferences: CatalogPreferences,
    private val libraryPreferences: LibraryPreferences,
    private val getSourcePager: (GetMangaPage) -> SourcePager,
    private val libraryActions: SourceLibraryActions,
    contextWrapper: ContextWrapper,
    private val savedStateHandle: SavedStateHandle,
    initialQuery: String?,
    startLatest: Boolean = false,
) : ViewModel(contextWrapper) {
    @Inject
    constructor(
        getLatestManga: GetLatestManga,
        getPopularManga: GetPopularManga,
        getSearchManga: GetSearchManga,
        catalogPreferences: CatalogPreferences,
        libraryPreferences: LibraryPreferences,
        getSourcePager: (GetMangaPage) -> SourcePager,
        libraryActions: SourceLibraryActions,
        contextWrapper: ContextWrapper,
        @Assisted savedStateHandle: SavedStateHandle,
        @Assisted params: Params,
    ) : this(
        params.source,
        getLatestManga,
        getPopularManga,
        getSearchManga,
        catalogPreferences,
        libraryPreferences,
        getSourcePager,
        libraryActions,
        contextWrapper,
        savedStateHandle,
        params.initialQuery,
        params.startLatest,
    )

    val displayMode = catalogPreferences.displayMode().stateIn(scope)
    val gridColumns = libraryPreferences.gridColumns().stateIn(scope)
    val gridSize = libraryPreferences.gridSize().stateIn(scope)

    private val _isLatest by savedStateHandle.getStateFlow { startLatest }
    val isLatest = _isLatest.asStateFlow()

    private val usingFilters by savedStateHandle.getStateFlow { false }
    private val filters = MutableStateFlow<List<SourceFilter>?>(null)

    private val _sourceSearchQuery by savedStateHandle.getStateFlow { initialQuery }
    val sourceSearchQuery = _sourceSearchQuery.asStateFlow()

    private val query = MutableStateFlow(sourceSearchQuery.value)

    private val pager = MutableStateFlow(getPager())

    val mangas = pager.flatMapLatest { it.mangas.map { mangas -> mangas.toImmutableList() } }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())
    val loading = pager.flatMapLatest { it.loading }
        .stateIn(scope, SharingStarted.Eagerly, true)
    val hasNextPage = pager.flatMapLatest { it.hasNextPage }
        .stateIn(scope, SharingStarted.Eagerly, true)
    val pageFailed = pager.flatMapLatest { it.pageFailed }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isSearching = combine(query, usingFilters) { query, usingFilters -> query != null || usingFilters }
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        pager.value.loadNextPage()
    }

    fun loadNextPage() {
        pager.value.loadNextPage()
    }

    /** Switching between popular and latest always leaves any search or filters behind. */
    fun setMode(toLatest: Boolean) {
        if (isLatest.value != toLatest || isSearching.value) {
            _isLatest.value = toLatest
            query.value = null
            _sourceSearchQuery.value = null
            usingFilters.value = false
            filters.value = null
            updatePager()
        }
    }

    private fun getPager(): SourcePager {
        val fetcher: suspend (page: Int) -> MangaPage? = when {
            query.value != null || usingFilters.value -> {
                { page ->
                    getSearchManga.await(
                        sourceId = source.id,
                        page = page,
                        searchTerm = query.value,
                        filters = filters.value,
                        onError = { toast(it.userMessage.toPlatformString()) },
                    )
                }
            }

            isLatest.value -> {
                { page ->
                    getLatestManga.await(
                        source,
                        page,
                        onError = { toast(it.userMessage.toPlatformString()) },
                    )
                }
            }

            else -> {
                { page ->
                    getPopularManga.await(
                        source.id,
                        page,
                        onError = { toast(it.userMessage.toPlatformString()) },
                    )
                }
            }
        }

        return getSourcePager(fetcher)
    }

    private fun updatePager() {
        pager.value.cancel()
        pager.value = getPager()
        pager.value.loadNextPage()
    }

    fun startSearch(query: String?) {
        this.query.value = query
        updatePager()
    }

    fun setUsingFilters(usingFilters: Boolean) {
        this.usingFilters.value = usingFilters
    }

    fun search(query: String) {
        _sourceSearchQuery.value = query
    }

    fun updateFilters(filters: List<SourceFilter>) {
        this.filters.value = filters
    }

    fun submitSearch() {
        startSearch(sourceSearchQuery.value)
    }

    val categories = libraryActions.categories(scope) { toast(it.userMessage.toPlatformString()) }

    fun addToLibrary(
        manga: Manga,
        categories: List<Category>,
    ) {
        scope.launch { libraryActions.add(manga, categories) { toast(it.userMessage.toPlatformString()) } }
    }

    fun removeFromLibrary(manga: Manga) {
        scope.launch { libraryActions.remove(manga) { toast(it.userMessage.toPlatformString()) } }
    }

    fun selectDisplayMode(displayMode: DisplayMode) {
        catalogPreferences.displayMode().set(displayMode)
    }

    data class Params(
        val source: Source,
        val initialQuery: String?,
        val startLatest: Boolean = false,
    )

    override fun onDispose() {
        super.onDispose()
        pager.value.cancel()
    }

    private companion object {
        private val log = logging()
    }
}
