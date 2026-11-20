/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.model.ChapterDisplay
import dev.bunko.domain.chapter.model.formatChapterNumber
import dev.bunko.i18n.MR
import dev.bunko.ui.base.chapter.ChapterDownloadIcon
import dev.bunko.ui.base.chapter.ChapterDownloadItem
import dev.bunko.ui.base.offline.OfflineState
import dev.bunko.uicore.components.selectedBackground
import dev.bunko.uicore.resources.stringResource
import kotlin.time.Instant

private const val SecondaryAlpha = 0.78f

private const val DisabledAlpha = 0.38f

@Composable
fun ChapterItem(
    chapterDownload: ChapterDownloadItem,
    format: (Instant) -> String,
    onClick: (Long) -> Unit,
    onClickDownload: (Long) -> Unit,
    onClickStopDownload: (Long) -> Unit,
    onClickDeleteChapter: (Long) -> Unit,
    offlineState: OfflineState?,
    onSaveOffline: (Long) -> Unit,
    onRemoveOffline: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
    onUnselectChapter: (Long) -> Unit,
    display: ChapterDisplay = ChapterDisplay.NAME,
) {
    val chapter = chapterDownload.chapter
    val isSelected by chapterDownload.isSelected.collectAsState()
    val inProgress = !chapter.read && chapter.lastPageRead > 0
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .selectedBackground(isSelected)
            .combinedClickable(
                onClick = { if (isSelected) onUnselectChapter(chapter.id) else onClick(chapter.id) },
                onLongClick = { onLongClick(chapter.id) },
            )
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!chapter.read) {
                    Icon(
                        imageVector = Icons.Filled.Circle,
                        contentDescription = null,
                        modifier = Modifier.height(8.dp).padding(end = 4.dp),
                        tint = primary,
                    )
                }
                if (chapter.bookmarked) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier.size(16.dp),
                        tint = primary.copy(alpha = if (chapter.read) DisabledAlpha else 1f),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    chapterTitle(chapter, display),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurface.copy(alpha = if (chapter.read) DisabledAlpha else 1f),
                )
            }
            val details = listOfNotNull(
                chapter.uploadDate.takeIf { it > 0 }?.let { format(Instant.fromEpochMilliseconds(it)) },
                chapter.scanlator?.takeIf { it.isNotBlank() },
            ).joinToString(" • ")
            if (inProgress || details.isNotEmpty()) {
                val subtitleColor: Color = onSurface.copy(alpha = if (chapter.read) DisabledAlpha else SecondaryAlpha)
                Row {
                    if (inProgress) {
                        Text(
                            stringResource(MR.strings.page_progress, chapter.lastPageRead + 1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = primary,
                        )
                        if (details.isNotEmpty()) {
                            Text(" • ", style = MaterialTheme.typography.bodySmall, color = subtitleColor)
                        }
                    }
                    if (details.isNotEmpty()) {
                        Text(
                            details,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                        )
                    }
                }
            }
        }

        OfflineChapterButton(
            state = offlineState,
            onSave = { onSaveOffline(chapter.id) },
            onRemove = { onRemoveOffline(chapter.id) },
        )

        ChapterDownloadIcon(
            chapterDownload,
            { onClickDownload(it.id) },
            { onClickStopDownload(it.id) },
            { onClickDeleteChapter(it.id) },
        )
    }
}

@Composable
private fun chapterTitle(
    chapter: Chapter,
    display: ChapterDisplay,
): String {
    val number = formatChapterNumber(chapter.chapterNumber).takeIf { display == ChapterDisplay.NUMBER }
    return if (number != null) stringResource(MR.strings.chapter_number_format, number) else chapter.name
}
