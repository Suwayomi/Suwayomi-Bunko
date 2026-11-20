/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.bunko.domain.chapter.model.ChapterDisplay
import dev.bunko.domain.chapter.model.ChapterListOptions
import dev.bunko.domain.chapter.model.ChapterSort
import dev.bunko.domain.library.model.FilterState
import dev.bunko.i18n.MR
import dev.bunko.ui.base.sheet.RadioItem
import dev.bunko.ui.base.sheet.SheetHeading
import dev.bunko.ui.base.sheet.SortItem
import dev.bunko.ui.base.sheet.TabbedSheet
import dev.bunko.ui.base.sheet.TriStateItem
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val ChapterSheetTabs = persistentListOf(
    MR.strings.action_filter,
    MR.strings.library_sort,
    MR.strings.library_display,
)

class ChapterSettingsActions(
    val setUnread: (FilterState) -> Unit,
    val setDownloaded: (FilterState) -> Unit,
    val setBookmarked: (FilterState) -> Unit,
    val setScanlatorShown: (String, Boolean) -> Unit,
    val showAllScanlators: () -> Unit,
    val setSort: (ChapterSort) -> Unit,
    val setDisplay: (ChapterDisplay) -> Unit,
    val setAsDefault: () -> Unit,
    val reset: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterSettingsSheet(
    options: ChapterListOptions,
    scanlators: ImmutableList<String>,
    actions: ChapterSettingsActions,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        TabbedSheet(
            tabs = ChapterSheetTabs,
            trailing = { OptionsMenu(actions) },
        ) { page ->
            when (page) {
                0 -> FilterPage(options, scanlators, actions)
                1 -> SortPage(options, actions.setSort)
                else -> DisplayPage(options.display, actions.setDisplay)
            }
        }
    }
}

@Composable
private fun OptionsMenu(actions: ChapterSettingsActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.chapter_options_set_default)) },
                onClick = {
                    actions.setAsDefault()
                    open = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.chapter_options_reset)) },
                onClick = {
                    actions.reset()
                    open = false
                },
            )
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    options: ChapterListOptions,
    scanlators: ImmutableList<String>,
    actions: ChapterSettingsActions,
) {
    TriStateItem(stringResource(MR.strings.filter_unread), options.unread, actions.setUnread)
    TriStateItem(stringResource(MR.strings.filter_downloaded), options.downloaded, actions.setDownloaded)
    TriStateItem(stringResource(MR.strings.action_filter_bookmarked), options.bookmarked, actions.setBookmarked)
    if (scanlators.size < 2) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SheetHeading(stringResource(MR.strings.scanlators), Modifier.weight(1f))
        if (options.excludedScanlators.isNotEmpty()) {
            TextButton(onClick = actions.showAllScanlators) {
                Text(stringResource(MR.strings.scanlators_show_all))
            }
        }
    }
    FlowRow(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        scanlators.fastForEach { scanlator ->
            val shown = scanlator !in options.excludedScanlators
            FilterChip(
                selected = shown,
                onClick = { actions.setScanlatorShown(scanlator, !shown) },
                label = { Text(scanlator, maxLines = 1) },
                leadingIcon = if (shown) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun SortPage(
    options: ChapterListOptions,
    setSort: (ChapterSort) -> Unit,
) {
    ChapterSort.entries.fastForEach { sort ->
        SortItem(
            label = stringResource(
                when (sort) {
                    ChapterSort.SOURCE -> MR.strings.chapter_sort_source
                    ChapterSort.NUMBER -> MR.strings.chapter_sort_number
                    ChapterSort.UPLOAD_DATE -> MR.strings.chapter_sort_upload_date
                    ChapterSort.ALPHABETICAL -> MR.strings.chapter_sort_alphabetical
                },
            ),
            descending = options.descending.takeIf { options.sort == sort },
            onClick = { setSort(sort) },
        )
    }
}

@Composable
private fun DisplayPage(
    display: ChapterDisplay,
    setDisplay: (ChapterDisplay) -> Unit,
) {
    ChapterDisplay.entries.fastForEach {
        RadioItem(
            label = stringResource(
                when (it) {
                    ChapterDisplay.NAME -> MR.strings.chapter_display_name
                    ChapterDisplay.NUMBER -> MR.strings.chapter_display_number
                },
            ),
            selected = display == it,
            onClick = { setDisplay(it) },
        )
    }
}
