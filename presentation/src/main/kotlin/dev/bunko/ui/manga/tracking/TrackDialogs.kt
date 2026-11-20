/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.tracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogContentPadding
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.dialog.SingleChoiceList
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.toImmutableList

// these dialogs are shown by their caller, so each one starts visible and reports every close
@Composable
private fun rememberShownDialog() = remember { DialogState(showing = true) }

@Composable
internal fun ChoiceDialog(
    title: String,
    choices: List<String>,
    selected: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val state = rememberShownDialog()
    AppDialog(
        state = state,
        title = title,
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onDismiss,
    ) {
        SingleChoiceList(choices.indices.map { it to choices[it] }.toImmutableList(), selected) {
            onSelect(it)
            onDismiss()
        }
    }
}

@Composable
internal fun ScoreDialog(
    scores: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val state = rememberShownDialog()
    AppDialog(
        state = state,
        title = stringResource(MR.strings.track_score),
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onDismiss,
    ) {
        FlowRow(
            modifier = DialogContentPadding.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            scores.forEach { score ->
                FilterChip(
                    selected = score == selected,
                    onClick = {
                        onSelect(score)
                        onDismiss()
                    },
                    label = { Text(score) },
                )
            }
        }
    }
}

@Composable
internal fun ChaptersDialog(
    current: Int,
    total: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberShownDialog()
    val max = if (total > 0) total else Int.MAX_VALUE
    var text by remember { mutableStateOf(current.toString()) }
    val value = text.toIntOrNull()?.takeIf { it in 0..max }
    AppDialog(
        state = state,
        title = stringResource(MR.strings.track_chapters),
        confirm = DialogButton(stringResource(MR.strings.action_ok), enabled = value != null) {
            value?.let(onConfirm)
            onDismiss()
        },
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onDismiss,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = DialogContentPadding.fillMaxWidth(),
        ) {
            FilledTonalIconButton(onClick = { text = ((value ?: current) - 1).coerceAtLeast(0).toString() }) {
                Icon(Icons.Filled.Remove, contentDescription = null)
            }
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(6) },
                singleLine = true,
                isError = value == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                supportingText = if (total > 0) {
                    { Text(stringResource(MR.strings.track_chapters_of, total), Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                } else {
                    null
                },
                modifier = Modifier.width(128.dp),
            )
            FilledTonalIconButton(onClick = { text = ((value ?: current) + 1).coerceAtMost(max).toString() }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    }
}

@Composable
internal fun RemoveDialog(
    trackerName: String,
    canDeleteRemote: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deleteRemote: Boolean) -> Unit,
) {
    val state = rememberShownDialog()
    var deleteRemote by remember { mutableStateOf(false) }
    AppDialog(
        state = state,
        title = stringResource(MR.strings.track_remove_title),
        confirm = DialogButton(stringResource(MR.strings.track_remove)) {
            onConfirm(deleteRemote)
            onDismiss()
        },
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onDismiss,
    ) {
        DialogMessage(stringResource(MR.strings.track_remove_message, trackerName))
        if (canDeleteRemote) {
            Row(
                Modifier.fillMaxWidth().clickable { deleteRemote = !deleteRemote }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = deleteRemote, onCheckedChange = null)
                Text(
                    stringResource(MR.strings.track_remove_remote, trackerName),
                    Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
