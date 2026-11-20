/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.filter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import dev.bunko.domain.source.model.sourcefilters.SourceFilter
import dev.bunko.domain.source.model.sourcefilters.SourceFilter.TriState.TriStateValue
import dev.bunko.i18n.MR
import dev.bunko.ui.sources.browse.filter.model.SourceFiltersView
import dev.bunko.ui.sources.browse.filter.model.activeCountFlow
import dev.bunko.ui.sources.browse.filter.model.next
import dev.bunko.uicore.resources.stringResource

private val Gutter = 16.dp

@Composable
fun FilterCountBadge(count: Int) {
    Badge(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) { Text(count.toString()) }
}

@Composable
internal fun FilterSectionHeader(
    title: String,
    views: List<SourceFiltersView<*, *>>,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count by remember(views) { views.activeCountFlow() }.collectAsState(0)
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Gutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) FilterCountBadge(count)
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
internal fun FilterHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = Gutter, vertical = 12.dp),
    )
}

@Composable
internal fun FilterDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier.padding(horizontal = Gutter, vertical = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectFilter(
    select: SourceFiltersView.Select,
    modifier: Modifier = Modifier,
) {
    val selected by select.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.padding(horizontal = Gutter, vertical = 6.dp),
    ) {
        OutlinedTextField(
            value = select.filter.values.getOrElse(selected) { "" },
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(select.name) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            select.filter.values.fastForEachIndexed { index, value ->
                DropdownMenuItem(
                    text = { Text(value, style = MaterialTheme.typography.labelLarge) },
                    onClick = {
                        select.updateState(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
internal fun TextFilter(
    text: SourceFiltersView.Text,
    modifier: Modifier = Modifier,
) {
    var value by remember(text) { mutableStateOf(text.state.value) }
    FilterTextField(
        value = value,
        onValueChange = {
            value = it
            text.updateState(it)
        },
        label = text.name,
        modifier = modifier,
    )
}

@Composable
internal fun FilterSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(MR.strings.action_searching),
        modifier = modifier,
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton({ onQueryChange("") }) { Icon(Icons.Rounded.Close, null) } }
        } else {
            null
        },
    )
}

@Composable
private fun FilterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterChips(
    views: List<SourceFiltersView<*, *>>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier.fillMaxWidth().padding(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        views.fastForEach {
            when (it) {
                is SourceFiltersView.CheckBox -> CheckBoxChip(it)
                is SourceFiltersView.TriState -> TriStateChip(it)
                else -> Unit
            }
        }
    }
}

@Composable
private fun CheckBoxChip(checkBox: SourceFiltersView.CheckBox) {
    val checked by checkBox.state.collectAsState()
    FilterChip(
        selected = checked,
        onClick = { checkBox.updateState(!checked) },
        label = { Text(checkBox.name) },
        leadingIcon = if (checked) chipIcon(Icons.Rounded.Check) else null,
        colors = includeColors(),
    )
}

@Composable
private fun TriStateChip(triState: SourceFiltersView.TriState) {
    val value by triState.state.collectAsState()
    val excluded = value == TriStateValue.EXCLUDE
    FilterChip(
        selected = value != TriStateValue.IGNORE,
        onClick = { triState.updateState(value.next()) },
        label = { Text(triState.name, textDecoration = if (excluded) TextDecoration.LineThrough else null) },
        leadingIcon = when (value) {
            TriStateValue.INCLUDE -> chipIcon(Icons.Rounded.Check)
            TriStateValue.EXCLUDE -> chipIcon(Icons.Rounded.Remove)
            TriStateValue.IGNORE -> null
        },
        colors = if (excluded) excludeColors() else includeColors(),
    )
}

private fun chipIcon(icon: ImageVector): @Composable () -> Unit =
    { Icon(icon, null, Modifier.size(FilterChipDefaults.IconSize)) }

@Composable
private fun includeColors(): SelectableChipColors =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

@Composable
private fun excludeColors(): SelectableChipColors =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
    )

@Composable
internal fun SortOption(
    sort: SourceFiltersView.Sort,
    index: Int,
    name: String,
    modifier: Modifier = Modifier,
) {
    val state by sort.state.collectAsState()
    val selected = state?.index == index
    val ascending = state?.ascending ?: false
    val rotation by animateFloatAsState(if (ascending) 0f else 180f)
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier.fillMaxWidth()
            .clickable { sort.updateState(SourceFilter.Sort.SelectionChange(selected && !ascending, index)) }
            .padding(horizontal = Gutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.ArrowUpward, null, tint = color, modifier = Modifier.rotate(rotation))
    }
}
