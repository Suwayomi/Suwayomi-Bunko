/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.chapter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.download.interactor.StopChapterDownload
import dev.bunko.domain.download.model.DownloadQueueItem
import dev.bunko.domain.download.model.DownloadState
import dev.bunko.domain.manga.model.Manga
import dev.bunko.i18n.MR
import dev.bunko.uicore.components.DropdownIconButton
import dev.bunko.uicore.resources.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Stable
data class ChapterDownloadItem(
    val manga: Manga?,
    val chapter: Chapter,
) {
    private val _isSelected = MutableStateFlow(false)
    val isSelected = _isSelected.asStateFlow()

    private val _downloadState: MutableStateFlow<ChapterDownloadState> = MutableStateFlow(
        when (chapter.downloaded) {
            true -> ChapterDownloadState.Downloaded
            false -> ChapterDownloadState.NotDownloaded
        },
    )
    val downloadState = _downloadState.asStateFlow()

    private val _downloadChapterFlow: MutableStateFlow<DownloadQueueItem?> = MutableStateFlow(null)
    val downloadChapterFlow = _downloadChapterFlow.asStateFlow()

    fun updateFrom(downloadingChapters: List<DownloadQueueItem>) {
        val downloadingChapter = downloadingChapters.find {
            it.chapter.id == chapter.id
        }
        if (downloadingChapter != null && downloadState.value != ChapterDownloadState.Downloading) {
            _downloadState.value = ChapterDownloadState.Downloading
        }
        if (downloadState.value == ChapterDownloadState.Downloading && downloadingChapter == null) {
            _downloadState.value = ChapterDownloadState.Downloaded
        }
        _downloadChapterFlow.value = downloadingChapter
    }

    suspend fun stopDownloading(stopChapterDownload: StopChapterDownload) {
        stopChapterDownload.await(chapter.id)
        _downloadState.value = ChapterDownloadState.NotDownloaded
    }

    fun setDownloaded(downloaded: Boolean) {
        _downloadState.value = if (downloaded) ChapterDownloadState.Downloaded else ChapterDownloadState.NotDownloaded
    }

    fun withChapter(chapter: Chapter) =
        ChapterDownloadItem(manga, chapter).also {
            it._downloadState.value = _downloadState.value
            it._downloadChapterFlow.value = _downloadChapterFlow.value
        }

    fun isSelected(selectedItems: List<Long>): Boolean = (chapter.id in selectedItems).also { _isSelected.value = it }
}

enum class ChapterDownloadState {
    NotDownloaded,
    Downloading,
    Downloaded,
}

@Composable
fun ChapterDownloadIcon(
    chapter: ChapterDownloadItem,
    onClickDownload: (Chapter) -> Unit,
    onClickStop: (Chapter) -> Unit,
    onClickDelete: (Chapter) -> Unit,
) {
    val downloadChapter by chapter.downloadChapterFlow.collectAsState()
    val downloadState by chapter.downloadState.collectAsState()

    when (downloadState) {
        ChapterDownloadState.Downloaded -> {
            DownloadedIconButton(
                chapter.chapter.mangaId to chapter.chapter.index,
                onClick = { onClickDelete(chapter.chapter) },
            )
        }

        ChapterDownloadState.Downloading -> {
            DownloadingIconButton(
                downloadChapter,
                onClick = { onClickStop(chapter.chapter) },
            )
        }

        ChapterDownloadState.NotDownloaded -> {
            DownloadIconButton(onClick = { onClickDownload(chapter.chapter) })
        }
    }
}

@Composable
private fun DownloadIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(1.5.dp, LocalContentColor.current.copy(alpha = DISABLED_ALPHA)),
        ) {
            Icon(
                Icons.Rounded.ArrowDownward,
                null,
                Modifier
                    .size(22.dp)
                    .padding(2.dp),
                LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
            )
        }
    }
}

@Composable
private fun DownloadingIconButton(
    downloadChapter: DownloadQueueItem?,
    onClick: () -> Unit,
) {
    DropdownIconButton(
        downloadChapter?.chapter?.id,
        { dismiss ->
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_cancel)) },
                onClick = {
                    dismiss()
                    onClick()
                },
            )
        },
    ) {
        when (downloadChapter?.state) {
            null, DownloadState.QUEUED -> CircularProgressIndicator(
                Modifier
                    .size(26.dp)
                    .padding(2.dp),
                LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                2.dp,
            )

            DownloadState.DOWNLOADING -> if (downloadChapter.progress != 0.0F) {
                val animatedProgress by animateFloatAsState(
                    targetValue = downloadChapter.progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .size(26.dp)
                        .padding(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )
                Icon(
                    Icons.Rounded.ArrowDownward,
                    null,
                    Modifier
                        .size(22.dp)
                        .padding(2.dp),
                    LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                )
            } else {
                CircularProgressIndicator(
                    Modifier
                        .size(26.dp)
                        .padding(2.dp),
                    LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                    2.dp,
                )
            }

            DownloadState.ERROR -> Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                Icon(
                    Icons.Rounded.Error,
                    null,
                    Modifier
                        .size(22.dp)
                        .padding(2.dp),
                    MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            DownloadState.FINISHED -> Surface(shape = CircleShape, color = LocalContentColor.current) {
                Icon(
                    Icons.Rounded.Check,
                    null,
                    Modifier
                        .size(22.dp)
                        .padding(2.dp),
                    MaterialTheme.colorScheme.surface,
                )
            }
        }
    }
}

@Composable
private fun DownloadedIconButton(
    chapter: Pair<Long, Int?>,
    onClick: () -> Unit,
) {
    DropdownIconButton(
        chapter,
        { dismiss ->
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_delete)) },
                onClick = {
                    dismiss()
                    onClick()
                },
            )
        },
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Icon(
                Icons.Rounded.Check,
                null,
                Modifier
                    .size(22.dp)
                    .padding(2.dp),
                MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
