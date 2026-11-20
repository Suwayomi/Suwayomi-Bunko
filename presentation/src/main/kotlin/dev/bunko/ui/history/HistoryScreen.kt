/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.history

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.bunko.i18n.MR
import dev.bunko.ui.history.components.ReadHistoryContent
import dev.bunko.ui.manga.MangaScreen
import dev.bunko.ui.reader.rememberReaderLauncher
import dev.bunko.ui.updates.UpdatesTab
import dev.bunko.ui.viewModel
import dev.bunko.uicore.resources.stringResource
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class HistoryScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        var tab by rememberSaveable { mutableIntStateOf(TAB_READ) }
        val tabs: @Composable () -> Unit = { HistoryTabs(tab) { tab = it } }
        when (tab) {
            TAB_READ -> ReadHistoryTab(tabs)
            else -> UpdatesTab(tabs)
        }
    }

    @Composable
    private fun ReadHistoryTab(tabs: @Composable () -> Unit) {
        val vm = viewModel { readHistoryViewModel() }
        val state by vm.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val readerLauncher = rememberReaderLauncher()
        ReadHistoryContent(
            tabs = tabs,
            state = state,
            onResume = { readerLauncher.launch(it.chapter.id, it.chapter.mangaId) },
            onOpenManga = { navigator push MangaScreen(it.chapter.mangaId) },
            onRemove = vm::remove,
            onClear = vm::clear,
            onRetry = { vm.reload() },
            onLoadMore = vm::loadMore,
        )
    }

    private companion object {
        const val TAB_READ = 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    PrimaryTabRow(selectedTabIndex = selected) {
        listOf(MR.strings.history_tab_read, MR.strings.history_tab_downloads).forEachIndexed { index, title ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                text = { Text(stringResource(title)) },
            )
        }
    }
}
