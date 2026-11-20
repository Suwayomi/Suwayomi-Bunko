/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DialogMessage(text: String) {
    Text(text, modifier = DialogContentPadding)
}

@Composable
fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else maxLines,
        isError = isError,
        visualTransformation = visualTransformation,
        modifier = DialogContentPadding.fillMaxWidth().then(modifier),
    )
}

@Composable
fun <T> SingleChoiceList(
    items: ImmutableList<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    LazyColumn(Modifier.heightIn(max = ListMaxHeight)) {
        items(items) { (key, label) ->
            ChoiceRow(
                label = label,
                modifier = Modifier.selectable(selected = key == selected, role = Role.RadioButton) { onSelect(key) },
            ) { RadioButton(selected = key == selected, onClick = null) }
        }
    }
}

@Composable
fun <T> MultiChoiceList(
    items: ImmutableList<Pair<T, String>>,
    checked: Set<T>,
    onToggle: (T) -> Unit,
) {
    LazyColumn(Modifier.heightIn(max = ListMaxHeight)) {
        items(items) { (key, label) ->
            ChoiceRow(
                label = label,
                modifier = Modifier.toggleable(value = key in checked, role = Role.Checkbox) { onToggle(key) },
            ) { Checkbox(checked = key in checked, onCheckedChange = null) }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    modifier: Modifier,
    control: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        control()
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private val ListMaxHeight = 420.dp
