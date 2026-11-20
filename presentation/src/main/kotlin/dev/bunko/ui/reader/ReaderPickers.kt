/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bunko.domain.manga.model.MangaMeta
import dev.bunko.domain.reader.model.DefaultReaderMode
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.uicore.resources.stringResource
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList

private val TileWidth = 96.dp

@Composable
fun ReaderModeGrid(
    modes: ImmutableList<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TileRow(modifier) {
        OptionTile(Icons.Outlined.RestartAlt, stringResource(MR.strings.default_reader_mode), selected == MangaMeta.DEFAULT_READER_MODE) {
            onSelect(MangaMeta.DEFAULT_READER_MODE)
        }
        modes.forEach { mode ->
            OptionTile(readerModeIcon(mode), readerModeLabel(mode), selected == mode) { onSelect(mode) }
        }
    }
}

@Composable
fun OrientationGrid(
    selected: OrientationLock,
    onSelect: (OrientationLock) -> Unit,
    modifier: Modifier = Modifier,
) {
    TileRow(modifier) {
        OrientationLock.entries.forEach { lock ->
            OptionTile(lock.icon(), stringResource(lock.label), selected == lock) { onSelect(lock) }
        }
    }
}

@Composable
fun ReaderModePickerDialog(
    state: DialogState,
    modes: ImmutableList<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    AppDialog(state = state, title = stringResource(MR.strings.reader_mode)) {
        ReaderModeGrid(modes, selected, { onSelect(it); state.hide() }, Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
fun OrientationPickerDialog(
    state: DialogState,
    selected: OrientationLock,
    onSelect: (OrientationLock) -> Unit,
) {
    AppDialog(state = state, title = stringResource(MR.strings.reader_orientation)) {
        OrientationGrid(selected, { onSelect(it); state.hide() }, Modifier.padding(horizontal = 16.dp))
    }
}

val OrientationLock.label: StringResource
    get() = when (this) {
        OrientationLock.Unlocked -> MR.strings.reader_orientation_free
        OrientationLock.Portrait -> MR.strings.reader_orientation_portrait
        OrientationLock.Landscape -> MR.strings.reader_orientation_landscape
    }

fun OrientationLock.icon(): ImageVector =
    when (this) {
        OrientationLock.Unlocked -> Icons.Outlined.ScreenRotation
        OrientationLock.Portrait -> Icons.Outlined.StayCurrentPortrait
        OrientationLock.Landscape -> Icons.Outlined.StayCurrentLandscape
    }

private fun readerModeIcon(mode: String): ImageVector =
    when (DefaultReaderMode.entries.find { it.res == mode }) {
        DefaultReaderMode.RTL -> Icons.Outlined.KeyboardDoubleArrowLeft
        DefaultReaderMode.LTR -> Icons.Outlined.KeyboardDoubleArrowRight
        DefaultReaderMode.Vertical -> Icons.Outlined.KeyboardDoubleArrowDown
        DefaultReaderMode.ContinuesVertical -> Icons.Outlined.ViewDay
        DefaultReaderMode.LongStrip -> Icons.Outlined.ViewAgenda
        null -> Icons.AutoMirrored.Outlined.MenuBook
    }

@Composable
private fun TileRow(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun OptionTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(TileWidth),
    ) {
        Column(
            Modifier.clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
