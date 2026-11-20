/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.tracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.track.model.TrackRecord
import dev.bunko.domain.track.model.TrackSearch
import dev.bunko.domain.track.model.TrackUpdate
import dev.bunko.domain.track.model.Tracker
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingSheet(
    state: TrackingState,
    search: TrackSearchUi?,
    mangaTitle: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: (Tracker, String) -> Unit,
    onSearch: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onBind: (TrackSearch) -> Unit,
    onUpdate: (TrackRecord, TrackUpdate) -> Unit,
    onUnbind: (TrackRecord, Boolean) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        if (search != null) {
            TrackSearchContent(search, onSearch, onCloseSearch, onBind)
        } else {
            TrackingHome(state, mangaTitle, onRetry, onOpenSettings, onOpenSearch, onUpdate, onUnbind)
        }
    }
}

@Composable
private fun TrackingHome(
    state: TrackingState,
    mangaTitle: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: (Tracker, String) -> Unit,
    onUpdate: (TrackRecord, TrackUpdate) -> Unit,
    onUnbind: (TrackRecord, Boolean) -> Unit,
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
        when (state) {
            TrackingState.Loading -> CircularProgressIndicator()

            is TrackingState.Failed -> SheetMessage(stringResource(state.message), stringResource(MR.strings.action_retry), onRetry)

            is TrackingState.Loaded -> if (state.items.isEmpty()) {
                SheetMessage(
                    stringResource(MR.strings.tracking_none_logged_in),
                    stringResource(MR.strings.tracking_open_settings),
                    onOpenSettings,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.tracker.id }) { item ->
                        TrackerRow(item, mangaTitle, onOpenSearch, onUpdate, onUnbind)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetMessage(
    message: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun TrackerRow(
    item: TrackerItem,
    mangaTitle: String,
    onOpenSearch: (Tracker, String) -> Unit,
    onUpdate: (TrackRecord, TrackUpdate) -> Unit,
    onUnbind: (TrackRecord, Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TrackerLogo(item.iconUrl, item.tracker.name)
        val record = item.record
        if (record == null) {
            OutlinedButton(
                onClick = { onOpenSearch(item.tracker, mangaTitle) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(MR.strings.tracking_add))
            }
        } else {
            TrackRecordCard(item.tracker, record, onUpdate, onUnbind, Modifier.weight(1f))
        }
    }
}

private enum class TrackDialog { Status, Chapters, Score, Remove }

@Composable
private fun TrackRecordCard(
    tracker: Tracker,
    record: TrackRecord,
    onUpdate: (TrackRecord, TrackUpdate) -> Unit,
    onUnbind: (TrackRecord, Boolean) -> Unit,
    modifier: Modifier,
) {
    var dialog by remember { mutableStateOf<TrackDialog?>(null) }
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    record.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (record.remoteUrl.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.action_open_in_browser)) },
                                onClick = {
                                    menuOpen = false
                                    runCatching { uriHandler.openUri(record.remoteUrl) }
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.track_remove)) },
                            onClick = {
                                menuOpen = false
                                dialog = TrackDialog.Remove
                            },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                TrackCell(
                    label = stringResource(MR.strings.track_status),
                    value = tracker.statuses.find { it.value == record.status }?.name,
                    onClick = { dialog = TrackDialog.Status },
                )
                VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 8.dp))
                TrackCell(
                    label = stringResource(MR.strings.track_chapters),
                    value = chapterProgress(record),
                    onClick = { dialog = TrackDialog.Chapters },
                )
                if (tracker.scores.isNotEmpty()) {
                    VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 8.dp))
                    TrackCell(
                        label = stringResource(MR.strings.track_score),
                        value = record.displayScore.takeIf { it.isNotBlank() },
                        onClick = { dialog = TrackDialog.Score },
                    )
                }
            }
        }
    }
    val dismiss = { dialog = null }
    when (dialog) {
        TrackDialog.Status -> ChoiceDialog(
            title = stringResource(MR.strings.track_status),
            choices = tracker.statuses.map { it.name },
            selected = tracker.statuses.indexOfFirst { it.value == record.status },
            onDismiss = dismiss,
            onSelect = { onUpdate(record, TrackUpdate(status = tracker.statuses[it].value)) },
        )

        TrackDialog.Score -> ScoreDialog(
            scores = tracker.scores,
            selected = record.displayScore,
            onDismiss = dismiss,
            onSelect = { onUpdate(record, TrackUpdate(score = it)) },
        )

        TrackDialog.Chapters -> ChaptersDialog(
            current = record.lastChapterRead.toInt(),
            total = record.totalChapters,
            onDismiss = dismiss,
            onConfirm = { onUpdate(record, TrackUpdate(lastChapterRead = it.toDouble())) },
        )

        TrackDialog.Remove -> RemoveDialog(
            trackerName = tracker.name,
            canDeleteRemote = tracker.supportsTrackDeletion,
            onDismiss = dismiss,
            onConfirm = { onUnbind(record, it) },
        )

        null -> Unit
    }
}

@Composable
private fun RowScope.TrackCell(
    label: String,
    value: String?,
    onClick: () -> Unit,
) {
    Column(
        Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value ?: stringResource(MR.strings.track_not_set),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun chapterProgress(record: TrackRecord): String {
    val read = record.lastChapterRead.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
    return if (record.totalChapters > 0) "$read / ${record.totalChapters}" else read
}
