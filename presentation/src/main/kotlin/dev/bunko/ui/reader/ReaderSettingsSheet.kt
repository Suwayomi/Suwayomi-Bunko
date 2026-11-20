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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bunko.domain.reader.model.ReaderBackground
import dev.bunko.i18n.MR
import dev.bunko.ui.base.sheet.SheetHeading
import dev.bunko.ui.base.sheet.TabbedSheet
import dev.bunko.uicore.resources.stringResource
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

private val ReaderSheetTabs = persistentListOf(
    MR.strings.reader_tab_mode,
    MR.strings.reader_tab_general,
    MR.strings.reader_tab_filter,
)

private const val FILTER_TAB = 2

private val BackgroundLabels: List<Pair<ReaderBackground, StringResource>> = listOf(
    ReaderBackground.Black to MR.strings.reader_background_black,
    ReaderBackground.Gray to MR.strings.reader_background_gray,
    ReaderBackground.White to MR.strings.reader_background_white,
    ReaderBackground.System to MR.strings.reader_background_system,
)

class ReaderSettingsState(
    val readerModes: ImmutableList<String>,
    val seriesReaderMode: String,
    val orientation: OrientationLock?,
    val background: ReaderBackground,
    val volumeKeys: Boolean,
    val dimmer: Float,
    val autoScrollSpeed: Int,
)

class ReaderSettingsActions(
    val setReaderMode: (String) -> Unit,
    val setOrientation: (OrientationLock) -> Unit,
    val setBackground: (ReaderBackground) -> Unit,
    val setVolumeKeys: (Boolean) -> Unit,
    val setDimmer: (Float) -> Unit,
    val setAutoScrollSpeed: (Int) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    state: ReaderSettingsState,
    actions: ReaderSettingsActions,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // the page stays undimmed on the filter tab so the effect can be judged while dragging
        scrimColor = if (page == FILTER_TAB) Color.Transparent else BottomSheetDefaults.ScrimColor,
    ) {
        TabbedSheet(ReaderSheetTabs, onPageChange = { page = it }) { tab ->
            when (tab) {
                0 -> ModePage(state, actions)
                1 -> GeneralPage(state, actions)
                else -> FilterPage(state, actions)
            }
        }
    }
}

@Composable
private fun ModePage(
    state: ReaderSettingsState,
    actions: ReaderSettingsActions,
) {
    SheetHeading(stringResource(MR.strings.reader_for_this_series))
    ReaderModeGrid(state.readerModes, state.seriesReaderMode, actions.setReaderMode, Modifier.padding(horizontal = 16.dp))
    if (state.orientation != null) {
        SheetHeading(stringResource(MR.strings.reader_orientation))
        OrientationGrid(state.orientation, actions.setOrientation, Modifier.padding(horizontal = 16.dp))
    }
    SheetHeading(stringResource(MR.strings.reader_background))
    FlowRow(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BackgroundLabels.forEach { (background, label) ->
            FilterChip(
                selected = background == state.background,
                onClick = { actions.setBackground(background) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun GeneralPage(
    state: ReaderSettingsState,
    actions: ReaderSettingsActions,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { actions.setVolumeKeys(!state.volumeKeys) }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(MR.strings.reader_volume_keys), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(state.volumeKeys, onCheckedChange = actions.setVolumeKeys)
    }
    SliderItem(
        label = stringResource(MR.strings.reader_auto_scroll_speed),
        valueLabel = state.autoScrollSpeed.toString(),
        value = state.autoScrollSpeed.toFloat(),
        range = AUTO_SCROLL_SPEEDS.first.toFloat()..AUTO_SCROLL_SPEEDS.last.toFloat(),
        onChange = { actions.setAutoScrollSpeed(it.roundToInt()) },
    )
}

@Composable
private fun FilterPage(
    state: ReaderSettingsState,
    actions: ReaderSettingsActions,
) {
    SliderItem(
        label = stringResource(MR.strings.reader_dimmer),
        valueLabel = "${(state.dimmer * 100).roundToInt()}%",
        value = state.dimmer,
        range = 0F..MAX_DIMMER,
        onChange = actions.setDimmer,
    )
}

@Composable
private fun SliderItem(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
