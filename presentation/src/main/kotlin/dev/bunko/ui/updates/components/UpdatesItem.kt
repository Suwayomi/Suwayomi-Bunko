/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.updates.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.ui.base.chapter.ChapterDownloadIcon
import dev.bunko.ui.base.chapter.ChapterDownloadItem
import dev.bunko.ui.base.chapter.ChapterDownloadState
import dev.bunko.ui.base.chapter.StorageBadges
import dev.bunko.i18n.MR
import dev.bunko.uicore.components.DropdownIconButton
import dev.bunko.uicore.resources.stringResource
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.uicore.components.MangaListItemImage
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.components.selectedBackground

private const val ReadAlpha = 0.38f

@Composable
fun UpdatesItem(
    chapterDownloadItem: ChapterDownloadItem,
    onDevice: Boolean,
    onClickItem: () -> Unit,
    onOpen: () -> Unit,
    onClickDeleteDevice: (Chapter) -> Unit,
    onLongClick: (Long) -> Unit,
    onUnselectChapter: (Long) -> Unit,
    onClickCover: () -> Unit,
    onClickDownload: (Chapter) -> Unit,
    onClickDeleteDownload: (Chapter) -> Unit,
    onClickStopDownload: (Chapter) -> Unit,
) {
    val manga = chapterDownloadItem.manga!!
    val chapter = chapterDownloadItem.chapter
    val alpha = if (chapter.read) ReadAlpha else 1f
    val isSelected by chapterDownloadItem.isSelected.collectAsState()
    val onServer = chapterDownloadItem.downloadState.collectAsState().value == ChapterDownloadState.Downloaded

    Row(
        modifier = Modifier
            .selectedBackground(isSelected)
            .combinedClickable(
                onClick = { if (isSelected) onUnselectChapter(chapter.id) else onClickItem() },
                onLongClick = { onLongClick(chapter.id) },
            )
            .heightIn(min = 64.dp)
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaListItemImage(
            modifier = Modifier
                .height(56.dp)
                .aspectRatio(mangaAspectRatio)
                .alpha(alpha)
                .libraryCoverFrame()
                .clickable { onClickCover() },
            data = manga,
            contentDescription = manga.title,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
                .alpha(alpha),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = manga.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ChapterLine(chapter)
            if (onDevice || onServer) StorageBadges(onDevice, onServer)
        }
        ChapterDownloadIcon(
            chapterDownloadItem,
            onClickDownload,
            onClickStopDownload,
            onClickDeleteDownload,
        )
        CopyMenu(
            key = chapter.id,
            onDevice = onDevice,
            onServer = onServer,
            onOpen = onOpen,
            onDeleteDevice = { onClickDeleteDevice(chapter) },
            onDeleteServer = { onClickDeleteDownload(chapter) },
        )
    }
}

@Composable
private fun CopyMenu(
    key: Long,
    onDevice: Boolean,
    onServer: Boolean,
    onOpen: () -> Unit,
    onDeleteDevice: () -> Unit,
    onDeleteServer: () -> Unit,
) {
    DropdownIconButton(
        key = key,
        dropdownItems = { dismiss ->
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_open_chapter)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null) },
                onClick = { dismiss(); onOpen() },
            )
            if (onDevice) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_delete_device_copy)) },
                    leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null) },
                    onClick = { dismiss(); onDeleteDevice() },
                )
            }
            if (onServer) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_delete_server_copy)) },
                    leadingIcon = { Icon(Icons.Rounded.Dns, null) },
                    onClick = { dismiss(); onDeleteServer() },
                )
            }
        },
    ) {
        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(MR.strings.action_more_actions))
    }
}

@Composable
private fun ChapterLine(chapter: Chapter) {
    val color = if (chapter.bookmarked && !chapter.read) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (chapter.bookmarked) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = chapter.name,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
