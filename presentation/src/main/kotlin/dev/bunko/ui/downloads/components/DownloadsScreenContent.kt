/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.downloads.components

import dev.bunko.uicore.components.EmptyScreen
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bunko.domain.download.model.DownloadChapter
import dev.bunko.domain.download.model.DownloadQueueItem
import dev.bunko.domain.download.model.DownloaderState
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.DropdownIconButton
import dev.bunko.uicore.components.MangaListItem
import dev.bunko.uicore.components.MangaListItemColumn
import dev.bunko.uicore.components.MangaListItemImage
import dev.bunko.uicore.components.MangaListItemSubtitle
import dev.bunko.uicore.components.MangaListItemTitle
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DownloadsScreenContent(
    downloadQueue: ImmutableList<DownloadQueueItem>,
    downloadStatus: DownloaderState,
    startDownloading: () -> Unit,
    pauseDownloading: () -> Unit,
    clearQueue: () -> Unit,
    onMangaClick: (Long) -> Unit,
    stopDownload: (DownloadChapter) -> Unit,
    moveDownloadUp: (DownloadChapter) -> Unit,
    moveDownloadDown: (DownloadChapter) -> Unit,
    moveDownloadToTop: (DownloadChapter) -> Unit,
    moveDownloadToBottom: (DownloadChapter) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.location_downloads))
        },
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            val state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = WindowInsets.bottomNav.add(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Bottom,
                    ),
                ).asPaddingValues(),
            ) {
                item(key = "queue-controls") {
                    QueueControls(
                        queueSize = downloadQueue.size,
                        downloadStatus = downloadStatus,
                        startDownloading = startDownloading,
                        pauseDownloading = pauseDownloading,
                        clearQueue = clearQueue,
                    )
                }
                items(downloadQueue, key = { it.chapter.id }) {
                    DownloadsItem(
                        modifier = Modifier.animateItem(),
                        item = it,
                        onClickCover = { onMangaClick(it.manga.id) },
                        onClickCancel = stopDownload,
                        onClickMoveUp = moveDownloadUp,
                        onClickMoveDown = moveDownloadDown,
                        onClickMoveToTop = moveDownloadToTop,
                        onClickMoveToBottom = moveDownloadToBottom,
                    )
                }
            }
            if (downloadQueue.isEmpty()) {
                EmptyScreen(stringResource(MR.strings.downloads_empty))
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
    }
}

@Composable
fun DownloadsItem(
    modifier: Modifier = Modifier,
    item: DownloadQueueItem,
    onClickCover: () -> Unit,
    onClickCancel: (DownloadChapter) -> Unit,
    onClickMoveUp: (DownloadChapter) -> Unit,
    onClickMoveDown: (DownloadChapter) -> Unit,
    onClickMoveToTop: (DownloadChapter) -> Unit,
    onClickMoveToBottom: (DownloadChapter) -> Unit,
) {
    MangaListItem(
        modifier = modifier
            .height(96.dp)
            .fillMaxWidth()
            .padding(end = 4.dp),
    ) {
        MangaListItemImage(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(mangaAspectRatio)
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                .libraryCoverFrame()
                .clickable { onClickCover() },
            data = item.manga,
            contentDescription = item.manga.title,
        )
        MangaListItemColumn(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            MangaListItemTitle(
                text = item.manga.title,
                fontWeight = FontWeight.SemiBold,
            )
            MangaListItemSubtitle(text = item.chapter.name)
            Spacer(Modifier.height(6.dp))
            val animatedProgress by animateFloatAsState(
                targetValue = item.progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            if (item.chapter.pageCount > 0) {
                Text(
                    text = stringResource(
                        MR.strings.downloads_pages,
                        (item.chapter.pageCount * item.progress).toInt(),
                        item.chapter.pageCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        IconButton(onClick = { onClickCancel(item.chapter) }) {
            Icon(
                Icons.Rounded.Close,
                stringResource(MR.strings.action_cancel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownIconButton(
            item.chapter.id,
            { dismiss ->
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_move_to_top)) },
                    onClick = {
                        dismiss()
                        onClickMoveToTop(item.chapter)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_move_up)) },
                    onClick = {
                        dismiss()
                        onClickMoveUp(item.chapter)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_move_down)) },
                    onClick = {
                        dismiss()
                        onClickMoveDown(item.chapter)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_move_to_bottom)) },
                    onClick = {
                        dismiss()
                        onClickMoveToBottom(item.chapter)
                    },
                )
            },
        ) {
            Icon(
                Icons.Rounded.MoreVert,
                null,
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun QueueControls(
    queueSize: Int,
    downloadStatus: DownloaderState,
    startDownloading: () -> Unit,
    pauseDownloading: () -> Unit,
    clearQueue: () -> Unit,
) {
    val running = downloadStatus == DownloaderState.STARTED
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(MR.strings.downloads_chapters_queued, queueSize),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!running) {
                    Text(
                        text = stringResource(MR.strings.downloads_paused),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalButton(onClick = if (running) pauseDownloading else startDownloading) {
                Icon(
                    if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (running) MR.strings.action_pause else MR.strings.action_continue))
            }
            IconButton(onClick = clearQueue, enabled = queueSize > 0) {
                Icon(Icons.Rounded.ClearAll, stringResource(MR.strings.action_clear_queue))
            }
        }
    }
}
