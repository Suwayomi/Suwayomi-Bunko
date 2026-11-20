/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.manga.model.Manga
import dev.bunko.i18n.MR
import dev.bunko.ui.base.chapter.ChapterDownloadItem
import dev.bunko.ui.base.offline.OfflineState
import dev.bunko.ui.base.chapter.ChapterDownloadState
import dev.bunko.ui.base.model.StableHolder
import dev.bunko.ui.base.navigation.Action
import dev.bunko.ui.base.navigation.ActionGroup
import dev.bunko.ui.base.navigation.ActionItem
import androidx.activity.compose.BackHandler
import dev.bunko.ui.base.navigation.OverflowMode
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.navigation.ToolbarDefault
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.manga.tracking.MangaTracking
import dev.bunko.ui.manga.tracking.TrackingSheet
import dev.bunko.ui.settings.SettingsTrackingScreen
import dev.bunko.ui.reader.rememberReaderLauncher
import dev.bunko.uicore.components.BottomActionItem
import dev.bunko.uicore.components.BottomActionMenu
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadingScreen
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.icons.BunkoAssets
import dev.bunko.uicore.icons.bunkoassets.DonePrev
import dev.bunko.uicore.resources.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Instant
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.rememberDialogState
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.model.ChapterListOptions
import dev.bunko.domain.chapter.model.ChapterSort
import dev.bunko.domain.chapter.model.chapterGap
import dev.bunko.domain.library.model.FilterState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaScreenContent(
    isLoading: Boolean,
    isRefreshing: Boolean,
    manga: Manga?,
    chapters: ImmutableList<ChapterDownloadItem>,
    displayChapters: ImmutableList<ChapterDownloadItem>,
    chapterOptions: ChapterListOptions,
    chapterOptionsCustomised: Boolean,
    scanlators: ImmutableList<String>,
    missingChapterCount: Int,
    resumeChapter: ChapterDownloadItem?,
    chapterSettingsActions: ChapterSettingsActions,
    dateTimeFormatter: (Instant) -> String,
    categoriesExist: Boolean,
    chooseCategoriesFlowHolder: StableHolder<SharedFlow<Unit>>,
    availableCategories: ImmutableList<Category>,
    mangaCategories: ImmutableList<Category>,
    inActionMode: Boolean,
    selectedItems: ImmutableList<ChapterDownloadItem>,
    addFavorite: (List<Category>, List<Category>) -> Unit,
    setCategories: () -> Unit,
    toggleFavorite: () -> Unit,
    refreshManga: () -> Unit,
    downloadNext: (Int) -> Unit,
    downloadUnread: () -> Unit,
    downloadAll: () -> Unit,
    markRead: (Long?) -> Unit,
    markUnread: (Long?) -> Unit,
    bookmarkChapter: (Long?) -> Unit,
    unBookmarkChapter: (Long?) -> Unit,
    markPreviousRead: (Int) -> Unit,
    downloadChapter: (Long) -> Unit,
    deleteDownload: (Long?) -> Unit,
    stopDownloadingChapter: (Long) -> Unit,
    onSelectChapter: (Long) -> Unit,
    onLongClickChapter: (Long) -> Unit,
    onUnselectChapter: (Long) -> Unit,
    selectAll: () -> Unit,
    invertSelection: () -> Unit,
    clearSelection: () -> Unit,
    downloadChapters: () -> Unit,
    offlineStates: Map<Long, OfflineState>,
    saveOffline: (Long?) -> Unit,
    removeOffline: (Long?) -> Unit,
    saveUnreadOffline: (Int?) -> Unit,
    loadChapters: () -> Unit,
    loadManga: () -> Unit,
    tracking: MangaTracking,
    onCoverBitmap: (Bitmap) -> Unit,
) {
    var trackingOpen by rememberSaveable { mutableStateOf(false) }
    val trackedCount by tracking.activeCount.collectAsState()
    val categoryDialogState = rememberDialogState()
    LaunchedEffect(Unit) {
        chooseCategoriesFlowHolder.item.collect {
            categoryDialogState.show()
        }
    }
    val readerLauncher = rememberReaderLauncher()

    BackHandler(inActionMode) {
        clearSelection()
    }

    var chapterSheetOpen by rememberSaveable { mutableStateOf(false) }
    val deleteDialogState = rememberDialogState()
    val uriHandler = LocalUriHandler.current
    val hasStarted = remember(chapters) {
        chapters.any { it.chapter.read || it.chapter.lastPageRead > 0 }
    }

    val listState = rememberLazyListState()
    val scrollProgress by remember { derivedStateOf { when { inActionMode -> 1f; listState.firstVisibleItemIndex > 0 -> 1f; else -> (listState.firstVisibleItemScrollOffset / 220f).coerceIn(0f, 1f) } } }
    val topBarColor = if (inActionMode) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer.copy(alpha = scrollProgress)
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier
            .onKeyEvent {
                if (inActionMode && it.type == KeyEventType.KeyUp && it.key == Key.Escape) {
                    clearSelection()
                    true
                } else {
                    false
                }
            }
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        topBar = {
            val navigator = LocalNavigator.current
            Toolbar(
                name = if (inActionMode) selectedItems.size.toString() else manga?.title.orEmpty(),
                isActionMode = inActionMode,
                containerColor = topBarColor,
                titleAlpha = if (inActionMode) 1f else scrollProgress,
                actions = {
                    if (inActionMode) {
                        getActionModeActionItems(
                            selectAll = selectAll,
                            invertSelection = invertSelection,
                        )
                    } else {
                        getActionItems(
                            refreshManga = refreshManga,
                            refreshMangaEnabled = !isLoading && !isRefreshing,
                            downloadNext = downloadNext,
                            downloadUnread = downloadUnread,
                            downloadAll = downloadAll,
                            saveUnreadOffline = saveUnreadOffline,
                        )
                    }
                },
                onClose = {
                    if (inActionMode) {
                        clearSelection()
                    } else {
                        navigator?.pop()
                    }
                },
                closeIcon = if (inActionMode) Icons.Rounded.Close else ToolbarDefault,
            )
        },
        bottomBar = {
            BottomActionMenu(
                visible = inActionMode,
                items = getBottomActionItems(
                    selectedItems = selectedItems,
                    markRead = { markRead(null) },
                    markUnread = { markUnread(null) },
                    bookmarkChapter = { bookmarkChapter(null) },
                    unBookmarkChapter = { unBookmarkChapter(null) },
                    markPreviousAsRead = markPreviousRead,
                    deleteChapter = deleteDialogState::show,
                    downloadChapters = downloadChapters,
                    offlineStates = offlineStates,
                    saveOffline = { saveOffline(null) },
                    removeOffline = { removeOffline(null) },
                ),
            )
        },
    ) { padding ->
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = refreshManga,
            state = pullState,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding()),
                )
            },
        ) {
            manga.let { manga ->
                if (manga != null) {
                    Box {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = WindowInsets.bottomNav.add(
                                WindowInsets.navigationBars.only(
                                    WindowInsetsSides.Bottom,
                                ),
                            ).add(WindowInsets(bottom = 88.dp)).asPaddingValues(),
                        ) {
                            item(key = "manga-header") {
                                MangaItem(
                                    manga = manga,
                                    categoriesVisible = categoriesExist && manga.inLibrary,
                                    favoriteEnabled = true,
                                    onToggleFavorite = toggleFavorite,
                                    onEditCategories = setCategories,
                                    onCoverBitmap = onCoverBitmap,
                                    topInset = padding.calculateTopPadding(),
                                    onOpenInBrowser = manga.realUrl?.let { url -> { uriHandler.openUri(url) } },
                                    trackedCount = trackedCount,
                                    onOpenTracking = {
                                        tracking.load()
                                        trackingOpen = true
                                    },
                                )
                            }
                            if (chapters.isNotEmpty()) {
                                item(key = "chapter-header") {
                                    ChapterHeader(
                                        shownCount = displayChapters.size,
                                        totalCount = chapters.size,
                                        missingCount = missingChapterCount,
                                        customised = chapterOptionsCustomised,
                                        onOpenSettings = { chapterSheetOpen = true },
                                        enabled = !inActionMode,
                                    )
                                }
                                val showGaps = chapterOptions.showsGaps()
                                displayChapters.forEachIndexed { index, chapter ->
                                    val gap = if (showGaps && index > 0) gapBetween(displayChapters[index - 1].chapter, chapter.chapter) else 0
                                    if (gap > 0) {
                                        item(key = "gap-${chapter.chapter.id}", contentType = "gap") {
                                            MissingChaptersRow(gap)
                                        }
                                    }
                                    item(key = chapter.chapter.id, contentType = "chapter") {
                                        ChapterItem(
                                            chapter,
                                            dateTimeFormatter,
                                            onClick = if (inActionMode) {
                                                {
                                                    if (chapter.isSelected.value) {
                                                        onUnselectChapter(it)
                                                    } else {
                                                        onSelectChapter(it)
                                                    }
                                                }
                                            } else {
                                                { readerLauncher.launch(it, manga.id) }
                                            },
                                            onClickDownload = downloadChapter,
                                            onClickDeleteChapter = deleteDownload,
                                            onClickStopDownload = stopDownloadingChapter,
                                            offlineState = offlineStates[chapter.chapter.id],
                                            onSaveOffline = saveOffline,
                                            onRemoveOffline = removeOffline,
                                            onLongClick = { if (inActionMode) onLongClickChapter(it) else onSelectChapter(it) },
                                            onUnselectChapter = onUnselectChapter,
                                            display = chapterOptions.display,
                                        )
                                    }
                                }
                            } else if (!isLoading) {
                                item {
                                    ErrorScreen(
                                        stringResource(MR.strings.no_chapters_found),
                                        Modifier.height(400.dp).fillMaxWidth(),
                                        retry = loadChapters,
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .windowInsetsPadding(
                                    WindowInsets.bottomNav.add(
                                        WindowInsets.navigationBars.only(
                                            WindowInsetsSides.Bottom,
                                        ),
                                    ),
                                ),
                            adapter = rememberScrollbarAdapter(listState),
                        )
                        val isAtTop by remember {
                            derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
                        }
                        if (resumeChapter != null && !inActionMode) {
                            ExtendedFloatingActionButton(
                                onClick = { readerLauncher.launch(resumeChapter.chapter.id, manga.id) },
                                expanded = isAtTop,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                                text = {
                                    Text(
                                        stringResource(
                                            if (hasStarted) MR.strings.action_resume else MR.strings.action_start,
                                        ),
                                    )
                                },
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .windowInsetsPadding(
                                        WindowInsets.bottomNav.add(
                                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                                        ),
                                    )
                                    .padding(16.dp),
                            )
                        }
                    }
                } else if (!isLoading) {
                    ErrorScreen(stringResource(MR.strings.failed_manga_fetch), retry = loadManga)
                }
            }
            if (isLoading) {
                LoadingScreen()
            }
        }
    }
    CategorySelectDialog(categoryDialogState, availableCategories, mangaCategories, addFavorite)
    AppDialog(
        state = deleteDialogState,
        title = null,
        confirm = DialogButton(stringResource(MR.strings.action_delete)) { deleteDownload(null) },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        DialogMessage(stringResource(MR.strings.delete_downloads_confirm, selectedItems.size))
    }
    if (chapterSheetOpen) {
        ChapterSettingsSheet(
            options = chapterOptions,
            scanlators = scanlators,
            actions = chapterSettingsActions,
            onDismiss = { chapterSheetOpen = false },
        )
    }
    if (trackingOpen && manga != null) {
        val navigator = LocalNavigator.current
        TrackingSheet(
            state = tracking.state.collectAsState().value,
            search = tracking.search.collectAsState().value,
            mangaTitle = manga.title,
            onDismiss = {
                tracking.closeSearch()
                trackingOpen = false
            },
            onRetry = tracking::load,
            onOpenSettings = {
                trackingOpen = false
                navigator?.push(SettingsTrackingScreen())
            },
            onOpenSearch = tracking::openSearch,
            onSearch = tracking::search,
            onCloseSearch = tracking::closeSearch,
            onBind = tracking::bind,
            onUpdate = tracking::update,
            onUnbind = tracking::unbind,
        )
    }
}

@Composable
@Stable
private fun getActionItems(
    refreshManga: () -> Unit,
    refreshMangaEnabled: Boolean,
    downloadNext: (Int) -> Unit,
    downloadUnread: () -> Unit,
    downloadAll: () -> Unit,
    saveUnreadOffline: (Int?) -> Unit,
): ImmutableList<Action> =
    listOf(
        ActionItem(
            name = stringResource(MR.strings.action_refresh_manga),
            icon = Icons.Rounded.Refresh,
            overflowMode = OverflowMode.NEVER_OVERFLOW,
            doAction = refreshManga,
            enabled = refreshMangaEnabled,
        ),
        ActionGroup(
            name = stringResource(MR.strings.action_download),
            icon = Icons.Rounded.Download,
            actions = listOf(
                ActionItem(
                    name = stringResource(MR.strings.download_1),
                    doAction = { downloadNext(1) },
                ),
                ActionItem(
                    name = stringResource(MR.strings.download_5),
                    doAction = { downloadNext(5) },
                ),
                ActionItem(
                    name = stringResource(MR.strings.download_10),
                    doAction = { downloadNext(10) },
                ),
                ActionItem(
                    name = stringResource(MR.strings.download_unread),
                    doAction = downloadUnread,
                ),
                ActionItem(
                    name = stringResource(MR.strings.download_all),
                    doAction = downloadAll,
                ),
                ActionItem(
                    name = stringResource(MR.strings.offline_save_next_5),
                    doAction = { saveUnreadOffline(5) },
                ),
                ActionItem(
                    name = stringResource(MR.strings.offline_save_next_10),
                    doAction = { saveUnreadOffline(10) },
                ),
                ActionItem(
                    name = stringResource(MR.strings.offline_save_unread),
                    doAction = { saveUnreadOffline(null) },
                ),
            ).toImmutableList(),
        ),
    ).toImmutableList()

@Composable
@Stable
private fun getActionModeActionItems(
    selectAll: () -> Unit,
    invertSelection: () -> Unit,
): ImmutableList<ActionItem> =
    listOf(
        ActionItem(
            name = stringResource(MR.strings.action_select_all),
            icon = Icons.Rounded.SelectAll,
            doAction = selectAll,
        ),
        ActionItem(
            name = stringResource(MR.strings.action_select_inverse),
            icon = Icons.Rounded.FlipToBack,
            doAction = invertSelection,
        ),
    ).toImmutableList()

@Composable
@Stable
private fun getBottomActionItems(
    selectedItems: ImmutableList<ChapterDownloadItem>,
    markRead: () -> Unit,
    markUnread: () -> Unit,
    bookmarkChapter: () -> Unit,
    unBookmarkChapter: () -> Unit,
    markPreviousAsRead: (Int) -> Unit,
    deleteChapter: () -> Unit,
    downloadChapters: () -> Unit,
    offlineStates: Map<Long, OfflineState>,
    saveOffline: () -> Unit,
    removeOffline: () -> Unit,
): ImmutableList<BottomActionItem> =
    listOfNotNull(
        BottomActionItem(
            name = stringResource(MR.strings.action_bookmark),
            icon = Icons.Rounded.BookmarkAdd,
            onClick = bookmarkChapter,
        ).takeIf { selectedItems.fastAny { !it.chapter.bookmarked } },
        BottomActionItem(
            name = stringResource(MR.strings.action_remove_bookmark),
            icon = Icons.Rounded.BookmarkRemove,
            onClick = unBookmarkChapter,
        ).takeIf { selectedItems.fastAny { it.chapter.bookmarked } },
        BottomActionItem(
            name = stringResource(MR.strings.action_mark_as_read),
            icon = Icons.Rounded.DoneAll,
            onClick = markRead,
        ).takeIf { selectedItems.fastAny { !it.chapter.read } },
        BottomActionItem(
            name = stringResource(MR.strings.action_mark_as_unread),
            icon = Icons.Rounded.RemoveDone,
            onClick = markUnread,
        ).takeIf { selectedItems.fastAny { it.chapter.read } },
        BottomActionItem(
            name = stringResource(MR.strings.action_mark_previous_read),
            icon = BunkoAssets.DonePrev,
            onClick = { markPreviousAsRead(selectedItems.first().chapter.index) },
        ).takeIf { selectedItems.size == 1 },
        BottomActionItem(
            name = stringResource(MR.strings.action_download),
            icon = Icons.Rounded.Download,
            onClick = downloadChapters,
        ).takeIf { selectedItems.fastAny { it.downloadState.value == ChapterDownloadState.NotDownloaded } },
        BottomActionItem(
            name = stringResource(MR.strings.action_delete),
            icon = Icons.Rounded.Delete,
            onClick = deleteChapter,
        ).takeIf { selectedItems.fastAny { it.downloadState.value == ChapterDownloadState.Downloaded } },
        BottomActionItem(
            name = stringResource(MR.strings.offline_save),
            icon = Icons.Rounded.PhoneAndroid,
            onClick = saveOffline,
        ).takeIf { selectedItems.fastAny { it.chapter.id !in offlineStates } },
        BottomActionItem(
            name = stringResource(MR.strings.offline_remove),
            icon = Icons.Rounded.PhoneAndroid,
            onClick = removeOffline,
        ).takeIf { selectedItems.fastAny { it.chapter.id in offlineStates } },
    ).toImmutableList()

// gaps only mean something when the list is in reading order and nothing besides scanlators is hidden
private fun ChapterListOptions.showsGaps(): Boolean =
    (sort == ChapterSort.SOURCE || sort == ChapterSort.NUMBER) &&
        unread == FilterState.IGNORED &&
        downloaded == FilterState.IGNORED &&
        bookmarked == FilterState.IGNORED

private fun gapBetween(
    previous: Chapter,
    current: Chapter,
): Int = if (previous.chapterNumber >= current.chapterNumber) chapterGap(previous, current) else chapterGap(current, previous)
