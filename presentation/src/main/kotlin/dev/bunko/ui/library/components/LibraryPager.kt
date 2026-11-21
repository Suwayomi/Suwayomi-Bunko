/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.rounded.SearchOff
import dev.bunko.i18n.MR
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.resources.stringResource
import dev.bunko.ui.base.userMessage
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import dev.bunko.domain.manga.model.Manga
import dev.bunko.uicore.theme.BunkoMotion
import kotlinx.collections.immutable.persistentListOf
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.library.model.DisplayMode
import dev.bunko.ui.library.CategoryState
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadStateCrossfade
import kotlinx.collections.immutable.ImmutableList

@Composable
fun LibraryPager(
    pagerState: PagerState,
    categories: ImmutableList<Category>,
    displayMode: DisplayMode,
    gridColumns: Int,
    gridSize: Int,
    getLibraryForPage: @Composable (Long) -> State<CategoryState>,
    selection: Set<Long>,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Long) -> Unit,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
    onGlobalSearch: (() -> Unit)?,
) {
    if (categories.isEmpty()) return

    HorizontalPager(state = pagerState) { page ->
        val state = getLibraryForPage(categories[page].id).value
        val content: @Composable (CategoryState) -> Unit = { library ->
            when (library) {
                CategoryState.Loading -> LibrarySkeleton(displayMode, gridColumns, gridSize)

                is CategoryState.Failed -> ErrorScreen(stringResource(library.e.userMessage))

                is CategoryState.Loaded -> LibraryLoadedPage(
                    library = library,
                    displayMode = displayMode,
                    gridColumns = gridColumns,
                    gridSize = gridSize,
                    selection = selection,
                    onClickManga = onClickManga,
                    onLongClickManga = onLongClickManga,
                    showUnread = showUnread,
                    showDownloaded = showDownloaded,
                    showLanguage = showLanguage,
                    showLocal = showLocal,
                    onGlobalSearch = onGlobalSearch,
                )
            }
        }
        val mountedLoaded = remember { state is CategoryState.Loaded }
        if (mountedLoaded) content(state) else LoadStateCrossfade(state, content = content)
    }
}

@Composable
private fun LibraryLoadedPage(
    library: CategoryState.Loaded,
    displayMode: DisplayMode,
    gridColumns: Int,
    gridSize: Int,
    selection: Set<Long>,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Long) -> Unit,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
    onGlobalSearch: (() -> Unit)?,
) {
    val current by library.items.collectAsState()
    val lastShown = remember { LastShown() }
    if (current.isNotEmpty()) lastShown.items = current
    val empty = current.isEmpty()
    val emptyAlpha = animateFloatAsState(
        targetValue = if (empty) 1f else 0f,
        animationSpec = tween(BunkoMotion.MEDIUM_MS),
        label = "libraryEmpty",
    )
    val emptyNow by rememberUpdatedState(empty)
    val showGrid by remember { derivedStateOf { !emptyNow || emptyAlpha.value < 1f } }
    val showEmpty by remember { derivedStateOf { emptyNow || emptyAlpha.value > 0f } }
    Box {
        if (showGrid) {
            Box(Modifier.graphicsLayer { alpha = 1f - emptyAlpha.value }) {
                LibraryItems(
                    items = if (current.isEmpty()) lastShown.items else current,
                    displayMode = displayMode,
                    gridColumns = gridColumns,
                    gridSize = gridSize,
                    selection = selection,
                    onClickManga = onClickManga,
                    onLongClickManga = onLongClickManga,
                    showUnread = showUnread,
                    showDownloaded = showDownloaded,
                    showLanguage = showLanguage,
                    showLocal = showLocal,
                )
            }
        }
        if (showEmpty) {
            Box(Modifier.graphicsLayer { alpha = emptyAlpha.value }) {
                val nothingInCategory = library.unfilteredItems.collectAsState().value.isEmpty()
                if (nothingInCategory) {
                    EmptyScreen(stringResource(MR.strings.library_empty), icon = Icons.Outlined.CollectionsBookmark)
                } else {
                    EmptyScreen(
                        message = stringResource(MR.strings.library_no_matches),
                        icon = Icons.Rounded.SearchOff,
                        actionLabel = onGlobalSearch?.let { stringResource(MR.strings.action_search_globally) },
                        onAction = onGlobalSearch,
                    )
                }
            }
        }
    }
}

private class LastShown {
    var items: ImmutableList<Manga> = persistentListOf()
}

@Composable
private fun LibraryItems(
    items: ImmutableList<Manga>,
    displayMode: DisplayMode,
    gridColumns: Int,
    gridSize: Int,
    selection: Set<Long>,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Long) -> Unit,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
) {
    when (displayMode) {
        DisplayMode.CompactGrid -> LibraryMangaCompactGrid(
            library = items,
            gridColumns = gridColumns,
            gridSize = gridSize,
            selection = selection,
            onClickManga = onClickManga,
            onLongClickManga = onLongClickManga,
            showUnread = showUnread,
            showDownloaded = showDownloaded,
            showLanguage = showLanguage,
            showLocal = showLocal,
        )

        DisplayMode.ComfortableGrid -> LibraryMangaComfortableGrid(
            library = items,
            gridColumns = gridColumns,
            gridSize = gridSize,
            selection = selection,
            onClickManga = onClickManga,
            onLongClickManga = onLongClickManga,
            showUnread = showUnread,
            showDownloaded = showDownloaded,
            showLanguage = showLanguage,
            showLocal = showLocal,
        )

        DisplayMode.CoverOnlyGrid -> LibraryMangaCoverOnlyGrid(
            library = items,
            gridColumns = gridColumns,
            gridSize = gridSize,
            selection = selection,
            onClickManga = onClickManga,
            onLongClickManga = onLongClickManga,
            showUnread = showUnread,
            showDownloaded = showDownloaded,
            showLanguage = showLanguage,
            showLocal = showLocal,
        )

        DisplayMode.List -> LibraryMangaList(
            library = items,
            selection = selection,
            onClickManga = onClickManga,
            onLongClickManga = onLongClickManga,
            showUnread = showUnread,
            showDownloaded = showDownloaded,
            showLanguage = showLanguage,
            showLocal = showLocal,
        )
    }
}
