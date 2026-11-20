/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.bunko.domain.source.model.Source
import dev.bunko.ui.base.screen.BaseScreen
import dev.bunko.ui.manga.MangaScreen
import dev.bunko.ui.sources.browse.components.SourceScreenContent
import dev.bunko.ui.sources.browse.components.rememberLibraryToggle
import dev.bunko.ui.sources.browse.filter.SourceFiltersViewModel
import dev.bunko.ui.sources.browse.filter.model.activeCount
import dev.bunko.ui.sources.browse.filter.model.toSourceFilter
import dev.bunko.ui.sources.settings.SourceSettingsScreen
import dev.bunko.ui.stateViewModel
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class SourceScreen(
    val source: Source,
    private val initialQuery: String? = null,
    private val startLatest: Boolean = false,
) : BaseScreen() {
    override val key: ScreenKey = source.id.toString()

    @Composable
    override fun Content() {
        val sourceVM = stateViewModel {
            sourceViewModel(it, SourceScreenViewModel.Params(source, initialQuery, startLatest))
        }
        val filterVM = stateViewModel {
            sourceFiltersViewModel(it, SourceFiltersViewModel.Params(source.id))
        }
        var activeFilters by rememberSaveable { mutableIntStateOf(0) }
        val filters by filterVM.filters.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val onMangaLongClick = rememberLibraryToggle(
            categories = sourceVM.categories.collectAsState().value,
            onAdd = sourceVM::addToLibrary,
            onRemove = sourceVM::removeFromLibrary,
        )
        SourceScreenContent(
            source = source,
            onMangaClick = { navigator push MangaScreen(it) },
            onMangaLongClick = onMangaLongClick,
            onBackClick = { navigator.pop() },
            onSourceSettingsClick = { navigator push SourceSettingsScreen(it) },
            displayMode = sourceVM.displayMode.collectAsState().value,
            gridColumns = sourceVM.gridColumns.collectAsState().value,
            gridSize = sourceVM.gridSize.collectAsState().value,
            mangas = sourceVM.mangas.collectAsState().value,
            hasNextPage = sourceVM.hasNextPage.collectAsState().value,
            pageFailed = sourceVM.pageFailed.collectAsState().value,
            isSearching = sourceVM.isSearching.collectAsState().value,
            loading = sourceVM.loading.collectAsState().value,
            isLatest = sourceVM.isLatest.collectAsState().value,
            showLatestButton = source.supportsLatest,
            sourceSearchQuery = sourceVM.sourceSearchQuery.collectAsState().value,
            search = sourceVM::search,
            submitSearch = sourceVM::submitSearch,
            setMode = { latest ->
                if (activeFilters > 0) filterVM.resetFilters()
                activeFilters = 0
                sourceVM.setMode(latest)
            },
            loadNextPage = sourceVM::loadNextPage,
            retry = {
                sourceVM.loadNextPage()
                if (!filterVM.loading.value && filterVM.filters.value.isEmpty()) filterVM.resetFilters()
            },
            onSelectDisplayMode = sourceVM::selectDisplayMode,
            filters = filters,
            activeFilters = activeFilters,
            showingFilters = filterVM.showingFilters.collectAsState().value,
            showFilterButton = filterVM.filterButtonEnabled.collectAsState().value,
            setShowingFilters = filterVM::showingFilters,
            onApplyFilters = {
                val applied = filters.map { it.toSourceFilter() }
                activeFilters = applied.sumOf { it.activeCount() }
                sourceVM.updateFilters(applied)
                sourceVM.setUsingFilters(true)
                filterVM.showingFilters(false)
                sourceVM.submitSearch()
            },
            onResetFilters = {
                filterVM.resetFilters()
                sourceVM.updateFilters(emptyList())
                sourceVM.setUsingFilters(false)
                if (activeFilters > 0) sourceVM.submitSearch()
                activeFilters = 0
            },
        )
    }
}
