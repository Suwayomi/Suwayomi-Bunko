/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.history.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.rememberDialogState
import dev.bunko.ui.base.navigation.ActionItem
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.relativeDate
import dev.bunko.ui.history.HistoryEntry
import dev.bunko.ui.history.ReadHistoryViewModel.State
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadingScreen
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ReadHistoryContent(
    tabs: @Composable () -> Unit,
    state: State,
    onResume: (HistoryEntry) -> Unit,
    onOpenManga: (HistoryEntry) -> Unit,
    onRemove: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val clearDialog = rememberDialogState()
    val canClear = state is State.Loaded && state.days.isNotEmpty()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        ),
        topBar = {
            Column {
                val clearName = stringResource(MR.strings.history_clear)
                Toolbar(
                    stringResource(MR.strings.location_history),
                    actions = {
                        remember(canClear, clearName) {
                            persistentListOf(ActionItem(clearName, Icons.Rounded.DeleteSweep, enabled = canClear) { clearDialog.show() })
                        }
                    },
                )
                tabs()
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                State.Loading -> LoadingScreen()
                is State.Failed -> ErrorScreen(state.message, retry = onRetry)
                is State.Loaded -> if (state.isEmpty) {
                    EmptyScreen(stringResource(MR.strings.history_empty))
                } else {
                    HistoryList(state, onResume, onOpenManga, onRemove, onLoadMore)
                }
            }
        }
    }
    AppDialog(
        state = clearDialog,
        title = stringResource(MR.strings.history_clear),
        confirm = DialogButton(stringResource(MR.strings.history_clear), onClick = onClear),
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        DialogMessage(stringResource(MR.strings.history_clear_confirm))
    }
}

@Composable
private fun HistoryList(
    state: State.Loaded,
    onResume: (HistoryEntry) -> Unit,
    onOpenManga: (HistoryEntry) -> Unit,
    onRemove: (HistoryEntry) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - LOAD_AHEAD
        }
    }
    LaunchedEffect(nearEnd, state.hasMore) {
        if (nearEnd && state.hasMore) onLoadMore()
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.bottomNav.add(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)).asPaddingValues(),
    ) {
        if (state.pending.isNotEmpty()) {
            item(key = "pending") { DayHeader(stringResource(MR.strings.history_pending)) }
            items(state.pending, key = { "p${it.chapter.id}" }, contentType = { "entry" }) { entry ->
                HistoryRow(entry, { onResume(entry) }, { onOpenManga(entry) }, onRemove = null, modifier = Modifier.animateItem())
            }
        }
        state.days.forEach { day ->
            item(key = "d${day.date}") { DayHeader(relativeDate(day.date)) }
            items(day.entries, key = { "c${it.chapter.id}" }, contentType = { "entry" }) { entry ->
                HistoryRow(entry, { onResume(entry) }, { onOpenManga(entry) }, onRemove = { onRemove(entry) }, modifier = Modifier.animateItem())
            }
        }
    }
}

private const val LOAD_AHEAD = 5

@Composable
private fun DayHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}
