/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import dev.bunko.domain.library.model.FilterState

private const val DisabledAlpha = 0.38f

@Composable
fun SheetItem(
    label: String,
    onClick: (() -> Unit)?,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .alpha(if (onClick == null) DisabledAlpha else 1f)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { leading() }
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SheetHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
fun TriStateItem(
    label: String,
    state: FilterState,
    onClick: ((FilterState) -> Unit)?,
) {
    SheetItem(
        label = label,
        onClick = onClick?.let { { it(state.next()) } },
        leading = {
            Icon(
                imageVector = when (state) {
                    FilterState.INCLUDED -> Icons.Rounded.CheckBox
                    FilterState.EXCLUDED -> Icons.Rounded.DisabledByDefault
                    FilterState.IGNORED -> Icons.Rounded.CheckBoxOutlineBlank
                },
                contentDescription = null,
                tint = if (state == FilterState.IGNORED) LocalContentColor.current else MaterialTheme.colorScheme.primary,
            )
        },
    )
}

@Composable
fun CheckboxItem(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    SheetItem(
        label = label,
        onClick = onClick,
        leading = { Checkbox(checked = checked, onCheckedChange = null) },
    )
}

/** [descending] is null when this isn't the active sort. */
@Composable
fun SortItem(
    label: String,
    descending: Boolean?,
    onClick: () -> Unit,
) {
    SheetItem(
        label = label,
        onClick = onClick,
        leading = {
            if (descending != null) {
                Icon(
                    imageVector = if (descending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
fun RadioItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SheetItem(
        label = label,
        onClick = onClick,
        leading = { RadioButton(selected = selected, onClick = null) },
    )
}

fun FilterState.next(): FilterState =
    when (this) {
        FilterState.IGNORED -> FilterState.INCLUDED
        FilterState.INCLUDED -> FilterState.EXCLUDED
        FilterState.EXCLUDED -> FilterState.IGNORED
    }
