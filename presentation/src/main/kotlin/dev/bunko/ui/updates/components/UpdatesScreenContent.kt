/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.updates.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import dev.bunko.domain.base.WebsocketService
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.download.model.DownloadQueueItem
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.rememberDialogState
import dev.bunko.ui.base.relativeDate
import dev.bunko.i18n.MR
import dev.bunko.ui.base.chapter.ChapterDownloadItem
import dev.bunko.ui.base.chapter.ChapterDownloadState
import dev.bunko.ui.base.navigation.ActionItem
import androidx.activity.compose.BackHandler
import dev.bunko.ui.base.navigation.OverflowMode
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.navigation.ToolbarDefault
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.updates.UpdatesUI
import dev.bunko.uicore.components.BottomActionItem
import dev.bunko.uicore.components.BottomActionMenu
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.components.LoadingScreen
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreenContent(
    tabs: @Composable () -> Unit,
    downloadQueue: ImmutableList<DownloadQueueItem>,
    deviceSaving: Int,
    onDevice: (chapterId: Long) -> Boolean,
    deleteDeviceCopy: (Chapter) -> Unit,
    openQueue: () -> Unit,
    isLoading: Boolean,
    updates: ImmutableList<UpdatesUI>,
    inActionMode: Boolean,
    selectedItems: ImmutableList<ChapterDownloadItem>,
    loadNextPage: () -> Unit,
    openChapter: (id: Long, mangaId: Long) -> Unit,
    openManga: (Long) -> Unit,
    markRead: (Long?) -> Unit,
    markUnread: (Long?) -> Unit,
    bookmarkChapter: (Long?) -> Unit,
    unBookmarkChapter: (Long?) -> Unit,
    downloadChapter: (Chapter?) -> Unit,
    deleteDownloadedChapter: (Chapter?) -> Unit,
    stopDownloadingChapter: (Chapter) -> Unit,
    onSelectChapter: (Long) -> Unit,
    onLongClickChapter: (Long) -> Unit,
    onUnselectChapter: (Long) -> Unit,
    selectAll: () -> Unit,
    invertSelection: () -> Unit,
    clearSelection: () -> Unit,
    onUpdateLibrary: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    updateWebsocketStatus: WebsocketService.Status,
    restartLibraryUpdates: () -> Unit,
) {
    BackHandler(inActionMode) {
        clearSelection()
    }
    val deleteDialog = rememberDialogState()
    AppDialog(
        state = deleteDialog,
        title = null,
        confirm = DialogButton(stringResource(MR.strings.action_delete)) { deleteDownloadedChapter(null) },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        DialogMessage(stringResource(MR.strings.delete_downloads_confirm, selectedItems.size))
    }

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
                WindowInsets.statusBars.add(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
                ),
            ),
        topBar = {
            val navigator = LocalNavigator.current
            Column {
            Toolbar(
                if (inActionMode) selectedItems.size.toString() else stringResource(MR.strings.location_history),
                actions = {
                    if (inActionMode) {
                        getActionModeActionItems(
                            selectAll = selectAll,
                            invertSelection = invertSelection,
                        )
                    } else {
                        getActionItems(
                            updateWebsocketStatus,
                            restartLibraryUpdates,
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
                isActionMode = inActionMode,
            )
            tabs()
            }
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
                    deleteChapter = deleteDialog::show,
                    downloadChapters = { downloadChapter(null) },
                ),
            )
        },
    ) {
        Box(Modifier.fillMaxSize().padding(it)) {
            if (isLoading) {
                LoadingScreen()
            } else if (updates.isEmpty() && downloadQueue.isEmpty() && deviceSaving == 0) {
                EmptyScreen(
                    message = stringResource(MR.strings.updates_empty),
                    icon = Icons.Outlined.NewReleases,
                    actionLabel = stringResource(MR.strings.action_update_library),
                    onAction = onUpdateLibrary,
                )
            } else {
                val state = rememberLazyListState()
                val headerCounts = remember(updates) { updates.countsPerHeader() }
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        contentPadding = WindowInsets.bottomNav.add(
                            WindowInsets.navigationBars.only(
                                WindowInsetsSides.Bottom,
                            ),
                        ).asPaddingValues(),
                    ) {
                        activeDownloads(downloadQueue, deviceSaving, openQueue)
                        if (updates.isNotEmpty()) {
                            item(key = "recent-header") { SectionHeader(stringResource(MR.strings.history_downloads_recent)) }
                        }
                        updates.forEachIndexed { index, item ->
                            when (item) {
                                is UpdatesUI.Header -> stickyHeader(contentType = "date") {
                                    DateHeader(formatUpdatesDate(item.date), headerCounts[index] ?: 0)
                                }

                                is UpdatesUI.Item -> item(contentType = "update") {
                                    if (index == updates.lastIndex) {
                                        LaunchedEffect(Unit) { loadNextPage() }
                                    }
                                    val manga = item.chapterDownloadItem.manga!!
                                    val chapter = item.chapterDownloadItem.chapter
                                    UpdatesItem(
                                        chapterDownloadItem = item.chapterDownloadItem,
                                        onDevice = onDevice(chapter.id),
                                        onOpen = { openChapter(chapter.id, manga.id) },
                                        onClickDeleteDevice = deleteDeviceCopy,
                                        onClickItem = if (inActionMode) {
                                            {
                                                if (item.chapterDownloadItem.isSelected.value) {
                                                    onUnselectChapter(
                                                        item.chapterDownloadItem.chapter.id,
                                                    )
                                                } else {
                                                    onSelectChapter(item.chapterDownloadItem.chapter.id)
                                                }
                                            }
                                        } else {
                                            { openChapter(chapter.id, manga.id) }
                                        },
                                        onLongClick = { if (inActionMode) onLongClickChapter(it) else onSelectChapter(it) },
                                        onUnselectChapter = onUnselectChapter,
                                        onClickCover = { openManga(manga.id) },
                                        onClickDownload = downloadChapter,
                                        onClickDeleteDownload = deleteDownloadedChapter,
                                        onClickStopDownload = stopDownloadingChapter,
                                    )
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    rememberScrollbarAdapter(state),
                    Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .windowInsetsPadding(
                            WindowInsets.bottomNav.add(
                                WindowInsets.navigationBars.only(
                                    WindowInsetsSides.Bottom,
                                ),
                            ),
                        ),
                )
            }
            if (!inActionMode) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(MR.strings.action_update_library)) },
                    icon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                    onClick = onUpdateLibrary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(
                            WindowInsets.bottomNav.add(
                                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                            ),
                        )
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun formatUpdatesDate(isoDate: String): String =
    runCatching { LocalDate.parse(isoDate) }.getOrNull()?.let { relativeDate(it) } ?: isoDate

@Composable
private fun DateHeader(
    text: String,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (count > 0) {
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) { Text(count.toString()) }
        }
    }
}

// how many updates follow each date header, keyed by the header's position
private fun List<UpdatesUI>.countsPerHeader(): Map<Int, Int> {
    val counts = HashMap<Int, Int>()
    var header = -1
    forEachIndexed { index, item ->
        when (item) {
            is UpdatesUI.Header -> header = index
            is UpdatesUI.Item -> if (header >= 0) counts[header] = (counts[header] ?: 0) + 1
        }
    }
    return counts
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

private const val ACTIVE_SHOWN = 5

private fun LazyListScope.activeDownloads(
    queue: ImmutableList<DownloadQueueItem>,
    deviceSaving: Int,
    openQueue: () -> Unit,
) {
    if (queue.isEmpty() && deviceSaving == 0) return
    item(key = "active-header") { SectionHeader(stringResource(MR.strings.history_downloads_active)) }
    items(queue.take(ACTIVE_SHOWN), key = { "active-${it.chapter.id}" }) { ActiveDownloadRow(it) }
    if (deviceSaving > 0) {
        item(key = "active-device") {
            Text(
                text = stringResource(MR.strings.offline_saving_count, deviceSaving),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    if (queue.isNotEmpty()) {
        item(key = "active-open") {
            TextButton(onClick = openQueue, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(stringResource(MR.strings.history_downloads_queue))
            }
        }
    }
}

@Composable
@Stable
private fun getActionItems(
    updateWebsocketStatus: WebsocketService.Status? = null,
    restartLibraryUpdates: (() -> Unit)? = null,
): ImmutableList<ActionItem> =
    listOfNotNull(
        if (updateWebsocketStatus == WebsocketService.Status.STOPPED && restartLibraryUpdates != null) {
            ActionItem(
                name = stringResource(MR.strings.action_restart_library),
                overflowMode = OverflowMode.ALWAYS_OVERFLOW,
                doAction = restartLibraryUpdates,
            )
        } else {
            null
        },
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
    deleteChapter: () -> Unit,
    downloadChapters: () -> Unit,
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
            name = stringResource(MR.strings.action_download),
            icon = Icons.Rounded.Download,
            onClick = downloadChapters,
        ).takeIf { selectedItems.fastAny { it.downloadState.value == ChapterDownloadState.NotDownloaded } },
        BottomActionItem(
            name = stringResource(MR.strings.action_delete),
            icon = Icons.Rounded.Delete,
            onClick = deleteChapter,
        ).takeIf { selectedItems.fastAny { it.downloadState.value == ChapterDownloadState.Downloaded } },
    ).toImmutableList()
