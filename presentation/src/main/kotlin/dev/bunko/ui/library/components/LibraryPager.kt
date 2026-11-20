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
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.library.model.DisplayMode
import dev.bunko.ui.library.CategoryState
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadingScreen
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

    HorizontalPager(state = pagerState) {
        when (val library = getLibraryForPage(categories[it].id).value) {
            CategoryState.Loading -> LoadingScreen()

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
    val items by library.items.collectAsState()
    if (items.isEmpty()) {
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
        return
    }
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
