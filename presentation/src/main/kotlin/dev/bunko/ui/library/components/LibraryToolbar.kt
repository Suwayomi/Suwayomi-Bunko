/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import androidx.activity.compose.BackHandler
import dev.bunko.uicore.resources.stringResource

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    mangaCount: Int?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeviceOnly: Boolean,
    onToggleOnDeviceOnly: () -> Unit,
    onClickFilter: () -> Unit,
    onClickUpdateLibrary: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    onClickRestartLibraryUpdates: (() -> Unit)?,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    if (selectedCount > 0) {
        LibrarySelectionToolbar(
            selectedCount = selectedCount,
            onClickUnselectAll = onClickUnselectAll,
            onClickSelectAll = onClickSelectAll,
            onClickInvertSelection = onClickInvertSelection,
        )
    } else {
        LibraryRegularToolbar(
            hasActiveFilters = hasActiveFilters,
            mangaCount = mangaCount,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onDeviceOnly = onDeviceOnly,
            onToggleOnDeviceOnly = onToggleOnDeviceOnly,
            onClickFilter = onClickFilter,
            onClickUpdateLibrary = onClickUpdateLibrary,
            onClickOpenRandomManga = onClickOpenRandomManga,
            onClickRestartLibraryUpdates = onClickRestartLibraryUpdates,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryRegularToolbar(
    hasActiveFilters: Boolean,
    mangaCount: Int?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeviceOnly: Boolean,
    onToggleOnDeviceOnly: () -> Unit,
    onClickFilter: () -> Unit,
    onClickUpdateLibrary: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    onClickRestartLibraryUpdates: (() -> Unit)?,
) {
    var searching by rememberSaveable { mutableStateOf(searchQuery.isNotEmpty()) }
    val closeSearch = {
        onSearchQueryChange("")
        searching = false
    }
    BackHandler(searching) { closeSearch() }

    TopAppBar(
        title = {
            if (searching) {
                SearchField(searchQuery, onSearchQueryChange)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(MR.strings.location_library),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, false),
                    )
                    if (mangaCount != null && mangaCount > 0) {
                        CountPill(mangaCount.toString(), Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        navigationIcon = {
            if (searching) {
                IconButton(onClick = closeSearch) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(MR.strings.action_close),
                    )
                }
            }
        },
        actions = {
            if (searching) {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(MR.strings.action_close),
                        )
                    }
                }
            } else {
                IconButton(onClick = { searching = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(MR.strings.action_search),
                    )
                }
            }
            IconButton(onClick = onToggleOnDeviceOnly) {
                Icon(
                    imageVector = Icons.Rounded.DownloadDone,
                    contentDescription = stringResource(MR.strings.library_on_device_only),
                    tint = if (onDeviceOnly) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            IconButton(onClick = onClickFilter) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = stringResource(MR.strings.action_filter),
                    tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            OverflowMenu(
                onClickUpdateLibrary = onClickUpdateLibrary,
                onClickOpenRandomManga = onClickOpenRandomManga,
                onClickRestartLibraryUpdates = onClickRestartLibraryUpdates,
            )
        },
    )
}

@Composable
private fun OverflowMenu(
    onClickUpdateLibrary: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    onClickRestartLibraryUpdates: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = null)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        OverflowItem(stringResource(MR.strings.action_update_library)) {
            expanded = false
            onClickUpdateLibrary()
        }
        OverflowItem(stringResource(MR.strings.action_open_random_manga)) {
            expanded = false
            onClickOpenRandomManga()
        }
        if (onClickRestartLibraryUpdates != null) {
            OverflowItem(stringResource(MR.strings.action_restart_library)) {
                expanded = false
                onClickRestartLibraryUpdates()
            }
        }
    }
}

@Composable
private fun OverflowItem(
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(text = { Text(text) }, onClick = onClick)
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val textColor = MaterialTheme.colorScheme.onSurface
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            if (query.isEmpty()) {
                Text(
                    text = stringResource(MR.strings.action_searching),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            innerTextField()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    BackHandler { onClickUnselectAll() }
    TopAppBar(
        title = { Text(selectedCount.toString()) },
        navigationIcon = {
            LabeledIconButton(stringResource(MR.strings.action_cancel), onClickUnselectAll) { Icon(Icons.Rounded.Close, contentDescription = null) }
        },
        actions = {
            LabeledIconButton(stringResource(MR.strings.action_select_all), onClickSelectAll) { Icon(Icons.Rounded.SelectAll, contentDescription = null) }
            LabeledIconButton(stringResource(MR.strings.action_select_inverse), onClickInvertSelection) { Icon(Icons.Rounded.FlipToBack, contentDescription = null) }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledIconButton(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, content = icon)
    }
}
