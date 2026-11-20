/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.components

import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.ui.util.fastForEach
import dev.bunko.uicore.components.EmptyScreen
import androidx.compose.material3.Text
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.domain.library.model.DisplayMode
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.source.model.Source
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.ActionItem
import androidx.activity.compose.BackHandler
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.sources.browse.filter.FilterCountBadge
import dev.bunko.ui.sources.browse.filter.SourceFiltersSheet
import dev.bunko.ui.sources.browse.filter.model.SourceFiltersView
import dev.bunko.ui.sources.components.SourceFilterChip
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadingScreen
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceScreenContent(
    source: Source,
    onMangaClick: (Long) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    onBackClick: () -> Unit,
    onSourceSettingsClick: (Long) -> Unit,
    displayMode: DisplayMode,
    gridColumns: Int,
    gridSize: Int,
    mangas: ImmutableList<Manga>,
    hasNextPage: Boolean,
    pageFailed: Boolean,
    isSearching: Boolean,
    loading: Boolean,
    isLatest: Boolean,
    showLatestButton: Boolean,
    sourceSearchQuery: String?,
    search: (String) -> Unit,
    submitSearch: () -> Unit,
    setMode: (Boolean) -> Unit,
    loadNextPage: () -> Unit,
    retry: () -> Unit,
    onSelectDisplayMode: (DisplayMode) -> Unit,
    filters: ImmutableList<SourceFiltersView<*, *>>,
    activeFilters: Int,
    showingFilters: Boolean,
    showFilterButton: Boolean,
    setShowingFilters: (Boolean) -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
) {
    BackHandler {
        onBackClick()
    }

    BackHandler(showingFilters) {
        setShowingFilters(false)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(
                source.name,
                closable = true,
                onClose = onBackClick,
                searchText = sourceSearchQuery,
                search = search,
                searchSubmit = submitSearch,
                actions = {
                    getActionItems(
                        isConfigurable = source.isConfigurable,
                        onSourceSettingsClick = { onSourceSettingsClick(source.id) },
                    )
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                SourceChipsRow(
                    isLatest = isLatest,
                    isSearching = isSearching,
                    showLatestButton = showLatestButton,
                    showFilterButton = showFilterButton,
                    showingFilters = showingFilters,
                    activeFilters = activeFilters,
                    displayMode = displayMode,
                    setMode = setMode,
                    setShowingFilters = setShowingFilters,
                    onSelectDisplayMode = onSelectDisplayMode,
                )
                Box(Modifier.weight(1f)) {
                    MangaTable(
                        displayMode = displayMode,
                        gridColumns = gridColumns,
                        gridSize = gridSize,
                        mangas = mangas,
                        isLoading = loading,
                        hasNextPage = hasNextPage,
                        pageFailed = pageFailed,
                        onLoadNextPage = loadNextPage,
                        onRetry = retry,
                        onMangaClick = onMangaClick,
                        onMangaLongClick = onMangaLongClick,
                    )
                }
            }
        }
    }

    if (showingFilters) {
        ModalBottomSheet(
            onDismissRequest = { setShowingFilters(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentWindowInsets = { WindowInsets.statusBars },
        ) {
            SourceFiltersSheet(filters = filters, onApply = onApplyFilters, onReset = onResetFilters)
        }
    }
}

@Composable
private fun SourceChipsRow(
    isLatest: Boolean,
    isSearching: Boolean,
    showLatestButton: Boolean,
    showFilterButton: Boolean,
    showingFilters: Boolean,
    activeFilters: Int,
    displayMode: DisplayMode,
    setMode: (Boolean) -> Unit,
    setShowingFilters: (Boolean) -> Unit,
    onSelectDisplayMode: (DisplayMode) -> Unit,
) {
    Column {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SourceFilterChip(
                    text = stringResource(MR.strings.popular),
                    selected = !isLatest && !isSearching,
                    onClick = { setMode(false) },
                    icon = Icons.Rounded.Favorite,
                )
                if (showLatestButton) {
                    SourceFilterChip(
                        text = stringResource(MR.strings.move_to_latest),
                        selected = isLatest && !isSearching,
                        onClick = { setMode(true) },
                        icon = Icons.Rounded.NewReleases,
                    )
                }
                if (showFilterButton && !isLatest) {
                    SourceFilterChip(
                        text = stringResource(MR.strings.action_filter),
                        selected = showingFilters || activeFilters > 0,
                        onClick = { setShowingFilters(true) },
                        icon = Icons.Rounded.FilterList,
                        trailing = if (activeFilters > 0) {
                            { FilterCountBadge(activeFilters) }
                        } else {
                            null
                        },
                    )
                }
            }
            DisplayModeMenu(displayMode, onSelectDisplayMode)
        }
        HorizontalDivider()
    }
}

@Composable
private fun DisplayModeMenu(
    displayMode: DisplayMode,
    onSelectDisplayMode: (DisplayMode) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                if (displayMode == DisplayMode.List) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                contentDescription = stringResource(MR.strings.display_mode),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DisplayMode.entries.fastForEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(mode.res)) },
                    onClick = {
                        onSelectDisplayMode(mode)
                        open = false
                    },
                    leadingIcon = { RadioButton(selected = mode == displayMode, onClick = null) },
                )
            }
        }
    }
}

@Composable
private fun MangaTable(
    displayMode: DisplayMode,
    gridColumns: Int,
    gridSize: Int,
    mangas: ImmutableList<Manga>,
    isLoading: Boolean,
    hasNextPage: Boolean,
    pageFailed: Boolean,
    onLoadNextPage: () -> Unit,
    onRetry: () -> Unit,
    onMangaClick: (Long) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    when {
        isLoading -> LoadingScreen()
        mangas.isEmpty() && hasNextPage -> ErrorScreen(stringResource(MR.strings.error_generic), retry = onRetry)
        mangas.isEmpty() -> EmptyScreen(
            message = stringResource(MR.strings.no_results_found),
            icon = Icons.Rounded.SearchOff,
            actionLabel = stringResource(MR.strings.action_retry),
            onAction = onRetry,
        )
        displayMode == DisplayMode.List -> SourceMangaList(
            mangas = mangas,
            onClickManga = onMangaClick,
            onLongClickManga = onMangaLongClick,
            hasNextPage = hasNextPage,
            pageFailed = pageFailed,
            onLoadNextPage = onLoadNextPage,
        )
        displayMode == DisplayMode.ComfortableGrid -> SourceMangaComfortableGrid(
            mangas = mangas,
            gridColumns = gridColumns,
            gridSize = gridSize,
            onClickManga = onMangaClick,
            onLongClickManga = onMangaLongClick,
            hasNextPage = hasNextPage,
            pageFailed = pageFailed,
            onLoadNextPage = onLoadNextPage,
        )
        else -> SourceMangaCompactGrid(
            mangas = mangas,
            gridColumns = gridColumns,
            gridSize = gridSize,
            onClickManga = onMangaClick,
            onLongClickManga = onMangaLongClick,
            hasNextPage = hasNextPage,
            pageFailed = pageFailed,
            onLoadNextPage = onLoadNextPage,
            showTitles = displayMode != DisplayMode.CoverOnlyGrid,
        )
    }
}

@Composable
private fun getActionItems(
    isConfigurable: Boolean,
    onSourceSettingsClick: () -> Unit,
): ImmutableList<ActionItem> =
    if (isConfigurable) {
        persistentListOf(
            ActionItem(
                name = stringResource(MR.strings.location_settings),
                icon = Icons.Rounded.Settings,
                doAction = onSourceSettingsClick,
            ),
        )
    } else {
        persistentListOf()
    }
