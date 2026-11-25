/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.migration

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.bunko.domain.manga.model.Manga
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogContentPadding
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.screen.BaseScreen
import dev.bunko.ui.manga.MangaScreen
import dev.bunko.ui.sources.browse.SourceScreen
import dev.bunko.ui.sources.globalsearch.GlobalSearchViewModel
import dev.bunko.ui.sources.globalsearch.components.GlobalSearchScreenContent
import dev.bunko.ui.stateViewModel
import dev.bunko.ui.viewModel
import dev.bunko.uicore.resources.stringResource
import kotlinx.coroutines.flow.MutableStateFlow

class MigrationSearchScreen(
    private val mangaId: Long,
    private val title: String,
) : BaseScreen() {
    @Composable
    override fun Content() {
        val search = stateViewModel {
            globalSearchViewModel(it, GlobalSearchViewModel.Params(title))
        }
        val vm = viewModel { migrationViewModel(MigrationViewModel.Params(mangaId)) }
        val navigator = LocalNavigator.currentOrThrow
        var target by remember { mutableStateOf<Manga?>(null) }

        LaunchedEffect(vm) {
            vm.finished.collect { migrated ->
                target = null
                navigator.pop()
                if (navigator.lastItem is MangaScreen) navigator.replace(MangaScreen(migrated.id))
            }
        }

        GlobalSearchScreenContent(
            sources = search.sources.collectAsState().value,
            isLoading = search.isLoading.collectAsState().value,
            results = search.results,
            query = search.query.collectAsState().value,
            pinnedOnly = search.pinnedOnly.collectAsState().value,
            setPinnedOnly = search::setPinnedOnly,
            setQuery = search::setQuery,
            submitSearch = search::startSearch,
            onSourceClick = { navigator push SourceScreen(it, search.query.value) },
            onMangaClick = { target = it },
            onMangaLongClick = { navigator push MangaScreen(it.id) },
            title = stringResource(MR.strings.migrate_title, title),
        )

        val running = vm.running.collectAsState().value
        target?.let { manga ->
            if (running) {
                MigrationRunningDialog()
            } else {
                val sources = search.sources.collectAsState().value
                MigrationDialog(
                    target = manga,
                    fromSource = vm.from.collectAsState().value?.source?.displayName,
                    toSource = manga.source?.displayName ?: sources.find { it.id == manga.sourceId }?.displayName,
                    chapters = vm.chapters,
                    categories = vm.categories,
                    tracking = vm.tracking,
                    deleteDownloads = vm.deleteDownloads,
                    onDismiss = { target = null },
                    onMigrate = { replace -> vm.migrate(manga, replace) },
                )
            }
        }
    }
}

@Composable
private fun MigrationDialog(
    target: Manga,
    fromSource: String?,
    toSource: String?,
    chapters: MutableStateFlow<Boolean>,
    categories: MutableStateFlow<Boolean>,
    tracking: MutableStateFlow<Boolean>,
    deleteDownloads: MutableStateFlow<Boolean>,
    onDismiss: () -> Unit,
    onMigrate: (replace: Boolean) -> Unit,
) {
    val state = remember { DialogState(showing = true) }
    AppDialog(
        state = state,
        title = stringResource(MR.strings.migrate_dialog_title, target.title),
        confirm = DialogButton(stringResource(MR.strings.action_migrate)) { onMigrate(true) },
        neutral = DialogButton(stringResource(MR.strings.migrate_copy)) { onMigrate(false) },
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onDismiss,
        dismissOnConfirm = false,
    ) {
        val unknown = stringResource(MR.strings.status_unknown)
        DialogMessage(
            stringResource(
                MR.strings.migrate_dialog_text,
                fromSource ?: unknown,
                toSource ?: unknown,
            ),
        )
        OptionRow(stringResource(MR.strings.migrate_option_chapters), chapters)
        OptionRow(stringResource(MR.strings.migrate_option_categories), categories)
        OptionRow(stringResource(MR.strings.migrate_option_tracking), tracking)
        OptionRow(stringResource(MR.strings.migrate_option_delete_downloads), deleteDownloads, stringResource(MR.strings.migrate_option_migrate_only))
    }
}

@Composable
private fun OptionRow(
    text: String,
    option: MutableStateFlow<Boolean>,
    supporting: String? = null,
) {
    val checked by option.collectAsState()
    Row(
        Modifier.fillMaxWidth().clickable { option.value = !checked }.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(text)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MigrationRunningDialog() {
    val state = remember { DialogState(showing = true) }
    AppDialog(state = state, title = null) {
        Row(
            DialogContentPadding.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Text(stringResource(MR.strings.migrate_running))
        }
    }
}
