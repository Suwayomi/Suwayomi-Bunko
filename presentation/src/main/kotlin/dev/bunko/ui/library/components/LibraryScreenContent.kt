/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import dev.bunko.domain.category.model.Category
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.main.components.HideBottomNav
import dev.bunko.ui.manga.components.CategorySelectDialog
import dev.bunko.uicore.components.BottomActionItem
import dev.bunko.uicore.components.BottomActionMenu
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.resources.stringResource
import dev.bunko.ui.base.userMessage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.bunko.domain.base.WebsocketService
import dev.bunko.domain.library.model.DisplayMode
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.library.CategoryState
import dev.bunko.ui.library.LibraryState
import dev.bunko.ui.library.ResumeEntry
import dev.bunko.ui.library.settings.LibrarySheet
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadingScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun LibraryScreenContent(
    libraryState: LibraryState,
    selectedCategoryIndex: Int,
    displayMode: DisplayMode,
    gridColumns: Int,
    gridSize: Int,
    query: String,
    updateQuery: (String) -> Unit,
    onDeviceOnly: Boolean,
    toggleOnDeviceOnly: () -> Unit,
    getLibraryForPage: @Composable (Long) -> State<CategoryState>,
    onPageChanged: (Int) -> Unit,
    resume: ResumeEntry?,
    onResume: (chapterId: Long, mangaId: Long) -> Unit,
    onClickManga: (Long) -> Unit,
    onRemoveMangaClicked: (Set<Long>) -> Unit,
    onDeleteDownloadsClicked: (Set<Long>) -> Unit,
    onSetRead: (Set<Long>, Boolean) -> Unit,
    onDownloadUnread: (Set<Long>) -> Unit,
    onSetCategories: (Set<Long>, List<Category>) -> Unit,
    editableCategories: ImmutableList<Category>,
    isRefreshing: Boolean,
    onRefreshCategory: (Int) -> Unit,
    onGlobalSearch: (String) -> Unit,
    onUpdateLibrary: () -> Unit,
    showingMenu: Boolean,
    setShowingMenu: (Boolean) -> Unit,
    libraryFilters: @Composable () -> Unit,
    librarySort: @Composable () -> Unit,
    libraryDisplay: @Composable () -> Unit,
    hasActiveFilters: Boolean,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
    updateWebsocketStatus: WebsocketService.Status,
    restartLibraryUpdates: () -> Unit,
) {
    val pagerState = rememberPagerState(selectedCategoryIndex) {
        (libraryState as? LibraryState.Loaded)?.categories?.size ?: 1
    }
    LaunchedEffect(pagerState.isScrollInProgress to pagerState.currentPage) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedCategoryIndex) {
            onPageChanged(pagerState.currentPage)
        }
    }
    LaunchedEffect(selectedCategoryIndex) {
        if (pagerState.currentPage != selectedCategoryIndex) {
            pagerState.animateScrollToPage(selectedCategoryIndex)
        }
    }

    var selection by remember { mutableStateOf(emptySet<Long>()) }
    var anchor by remember { mutableStateOf<Long?>(null) }
    val currentItems: List<Manga> = (libraryState as? LibraryState.Loaded)
        ?.categories
        ?.getOrNull(pagerState.currentPage)
        ?.let { category ->
            (getLibraryForPage(category.id).value as? CategoryState.Loaded)?.items?.collectAsState()?.value
        }
        ?: persistentListOf()
    val toggleSelection = { id: Long ->
        selection = if (id in selection) selection - id else selection + id
        anchor = id.takeIf { it in selection }
    }
    // a long press while selecting extends the selection from the last picked title
    val longPress = { id: Long ->
        val ids = currentItems.map { it.id }
        val from = anchor?.takeIf { it in selection }?.let(ids::indexOf) ?: -1
        val to = ids.indexOf(id)
        if (selection.isEmpty() || from < 0 || to < 0) {
            toggleSelection(id)
        } else {
            selection = selection + ids.subList(minOf(from, to), maxOf(from, to) + 1)
            anchor = id
        }
    }
    // selection isn't saved, so neither are the dialogs acting on it
    val removeDialog = remember { DialogState() }
    val deleteDialog = remember { DialogState() }
    val categoryDialog = remember { DialogState() }
    HideBottomNav(selection.isNotEmpty())

    ThinLibraryScreenContent(
        pagerState = pagerState,
        libraryState = libraryState,
        selectedCategoryIndex = selectedCategoryIndex,
        displayMode = displayMode,
        gridColumns = gridColumns,
        gridSize = gridSize,
        query = query,
        updateQuery = updateQuery,
        onDeviceOnly = onDeviceOnly,
        toggleOnDeviceOnly = toggleOnDeviceOnly,
        getLibraryForPage = getLibraryForPage,
        onPageChanged = onPageChanged,
        selection = selection,
        mangaCount = currentItems.size,
        resume = resume,
        onResume = onResume,
        onClickManga = { if (selection.isEmpty()) onClickManga(it) else toggleSelection(it) },
        onLongClickManga = longPress,
        onClickUnselectAll = { selection = emptySet() },
        onClickSelectAll = { selection = currentItems.mapTo(HashSet()) { it.id } },
        onClickInvertSelection = {
            selection = currentItems.mapNotNullTo(HashSet()) { manga -> manga.id.takeIf { it !in selection } }
        },
        bottomActions = libraryBottomActions(
            canChangeCategories = editableCategories.isNotEmpty(),
            markRead = {
                onSetRead(selection, true)
                selection = emptySet()
            },
            markUnread = {
                onSetRead(selection, false)
                selection = emptySet()
            },
            downloadUnread = {
                onDownloadUnread(selection)
                selection = emptySet()
            },
            changeCategories = categoryDialog::show,
            deleteDownloads = deleteDialog::show,
            remove = removeDialog::show,
        ),
        isRefreshing = isRefreshing,
        onRefresh = { onRefreshCategory(pagerState.currentPage) },
        onGlobalSearch = { onGlobalSearch(query) }.takeIf { query.isNotBlank() },
        onClickOpenRandomManga = { currentItems.randomOrNull()?.let { onClickManga(it.id) } },
        onUpdateLibrary = onUpdateLibrary,
        showingSheet = showingMenu,
        setShowingSheet = setShowingMenu,
        libraryFilters = libraryFilters,
        librarySort = librarySort,
        libraryDisplay = libraryDisplay,
        hasActiveFilters = hasActiveFilters,
        showUnread = showUnread,
        showDownloaded = showDownloaded,
        showLanguage = showLanguage,
        showLocal = showLocal,
        updateWebsocketStatus = updateWebsocketStatus,
        restartLibraryUpdates = restartLibraryUpdates,
    )

    AppDialog(
        state = removeDialog,
        title = null,
        confirm = DialogButton(stringResource(MR.strings.action_remove)) {
            onRemoveMangaClicked(selection)
            selection = emptySet()
        },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        DialogMessage(stringResource(MR.strings.remove_from_library_confirm, selection.size))
    }
    AppDialog(
        state = deleteDialog,
        title = null,
        confirm = DialogButton(stringResource(MR.strings.action_delete)) {
            onDeleteDownloadsClicked(selection)
            selection = emptySet()
        },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        DialogMessage(stringResource(MR.strings.library_delete_downloads_confirm, selection.size))
    }
    CategorySelectDialog(categoryDialog, editableCategories, persistentListOf()) { picked, _ ->
        onSetCategories(selection, picked)
        selection = emptySet()
    }
}

@Composable
private fun libraryBottomActions(
    canChangeCategories: Boolean,
    markRead: () -> Unit,
    markUnread: () -> Unit,
    downloadUnread: () -> Unit,
    changeCategories: () -> Unit,
    deleteDownloads: () -> Unit,
    remove: () -> Unit,
): ImmutableList<BottomActionItem> =
    listOfNotNull(
        BottomActionItem(stringResource(MR.strings.action_mark_as_read), Icons.Rounded.DoneAll, markRead),
        BottomActionItem(stringResource(MR.strings.action_mark_as_unread), Icons.Rounded.RemoveDone, markUnread),
        BottomActionItem(stringResource(MR.strings.action_download_unread), Icons.Rounded.Download, downloadUnread),
        BottomActionItem(stringResource(MR.strings.edit_categories), Icons.AutoMirrored.Outlined.Label, changeCategories)
            .takeIf { canChangeCategories },
        BottomActionItem(stringResource(MR.strings.action_delete_downloads), Icons.Rounded.DeleteSweep, deleteDownloads),
        BottomActionItem(stringResource(MR.strings.action_remove_favorite), Icons.Rounded.Delete, remove),
    ).toImmutableList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinLibraryScreenContent(
    pagerState: PagerState,
    libraryState: LibraryState,
    selectedCategoryIndex: Int,
    displayMode: DisplayMode,
    gridColumns: Int,
    gridSize: Int,
    query: String,
    updateQuery: (String) -> Unit,
    onDeviceOnly: Boolean,
    toggleOnDeviceOnly: () -> Unit,
    getLibraryForPage: @Composable (Long) -> State<CategoryState>,
    onPageChanged: (Int) -> Unit,
    selection: Set<Long>,
    mangaCount: Int,
    resume: ResumeEntry?,
    onResume: (chapterId: Long, mangaId: Long) -> Unit,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Long) -> Unit,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    bottomActions: ImmutableList<BottomActionItem>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onGlobalSearch: (() -> Unit)?,
    onClickOpenRandomManga: () -> Unit,
    onUpdateLibrary: () -> Unit,
    showingSheet: Boolean,
    setShowingSheet: (Boolean) -> Unit,
    libraryFilters: @Composable () -> Unit,
    librarySort: @Composable () -> Unit,
    libraryDisplay: @Composable () -> Unit,
    hasActiveFilters: Boolean,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
    updateWebsocketStatus: WebsocketService.Status,
    restartLibraryUpdates: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        contentWindowInsets = WindowInsets(0),
        bottomBar = { BottomActionMenu(visible = selection.isNotEmpty(), items = bottomActions) },
        topBar = {
            Column {
                LibraryToolbar(
                    hasActiveFilters = hasActiveFilters,
                    selectedCount = selection.size,
                    mangaCount = mangaCount,
                    searchQuery = query,
                    onSearchQueryChange = updateQuery,
                    onDeviceOnly = onDeviceOnly,
                    onToggleOnDeviceOnly = toggleOnDeviceOnly,
                    onClickFilter = { setShowingSheet(true) },
                    onClickUpdateLibrary = onUpdateLibrary,
                    onClickOpenRandomManga = onClickOpenRandomManga,
                    onClickRestartLibraryUpdates = restartLibraryUpdates.takeIf {
                        updateWebsocketStatus == WebsocketService.Status.STOPPED
                    },
                    onClickUnselectAll = onClickUnselectAll,
                    onClickSelectAll = onClickSelectAll,
                    onClickInvertSelection = onClickInvertSelection,
                )
                if (libraryState is LibraryState.Loaded) {
                    LibraryTabs(
                        pagerState = pagerState,
                        categories = libraryState.categories,
                        onPageChanged = onPageChanged,
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (resume != null && libraryState is LibraryState.Loaded && query.isBlank()) {
                ResumeCard(resume, onResume)
            }
            Box(Modifier.weight(1f)) {
                when (libraryState) {
                    LibraryState.Loading -> {
                        LoadingScreen(true)
                    }

                    LibraryState.Empty -> EmptyScreen(stringResource(MR.strings.library_empty))

                    is LibraryState.Failed -> {
                        ErrorScreen(stringResource(libraryState.e.userMessage))
                    }

                    is LibraryState.Loaded -> PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                        LibraryPager(
                            pagerState = pagerState,
                            categories = libraryState.categories,
                            displayMode = displayMode,
                            gridColumns = gridColumns,
                            gridSize = gridSize,
                            getLibraryForPage = getLibraryForPage,
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
        }
    }

    if (showingSheet) {
        ModalBottomSheet(
            onDismissRequest = { setShowingSheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            LibrarySheet(
                libraryFilters = libraryFilters,
                librarySort = librarySort,
                libraryDisplay = libraryDisplay,
            )
        }
    }
}
