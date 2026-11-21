/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.bunko.i18n.MR
import dev.bunko.ui.base.file.rememberNamedFileChooser
import dev.bunko.ui.base.navigation.ActionItem
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.screen.BaseScreen
import dev.bunko.ui.extensions.components.ExtensionsScreenContent
import dev.bunko.ui.extensions.components.LanguageDialog
import dev.bunko.ui.sources.browse.SourceScreen
import dev.bunko.ui.sources.globalsearch.GlobalSearchScreen
import dev.bunko.ui.sources.home.components.SourceHomeScreenContent
import dev.bunko.ui.stateViewModel
import dev.bunko.ui.viewModel
import dev.bunko.uicore.resources.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import dev.bunko.ui.base.dialog.rememberDialogState

@OptIn(ExperimentalMaterial3Api::class)
class BrowseScreen(
    private val initialTab: Int = 0,
) : BaseScreen() {
    @Composable
    override fun Content() {
        val sourcesVm = stateViewModel { sourceHomeViewModel(it) }
        val extensionsVm = viewModel { extensionsViewModel() }
        val navigator = LocalNavigator.currentOrThrow
        val extensionsState by extensionsVm.state.collectAsState()

        val sourcesLanguageDialog = rememberDialogState()
        val extensionsLanguageDialog = rememberDialogState()
        val chooser = rememberNamedFileChooser(extensionsVm::install)

        val pagerState = rememberPagerState(initialTab) { 2 }
        val scope = rememberCoroutineScope()
        val tabs = listOf(MR.strings.location_sources, MR.strings.location_extensions)

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.statusBars.add(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
                ),
            ),
            topBar = {
                Column {
                    key(pagerState.currentPage) {
                        if (pagerState.currentPage == 0) {
                            val query = sourcesVm.query.collectAsState().value
                            Toolbar(
                                stringResource(MR.strings.location_browse),
                                closable = false,
                                searchText = query,
                                search = sourcesVm::setQuery,
                                searchSubmit = {
                                    if (query.isNotBlank()) navigator push GlobalSearchScreen(query)
                                },
                                actions = {
                                    persistentListOf(
                                        ActionItem(
                                            stringResource(MR.strings.enabled_languages),
                                            Icons.Rounded.Translate,
                                            doAction = sourcesLanguageDialog::show,
                                        ),
                                    )
                                },
                            )
                        } else {
                            Toolbar(
                                stringResource(MR.strings.location_browse),
                                closable = false,
                                searchText = extensionsState.searchQuery,
                                search = extensionsVm::setQuery,
                                actions = {
                                    persistentListOf(
                                        ActionItem(
                                            stringResource(MR.strings.enabled_languages),
                                            Icons.Rounded.Translate,
                                            doAction = extensionsLanguageDialog::show,
                                        ),
                                        ActionItem(
                                            stringResource(MR.strings.action_install),
                                            Icons.Rounded.Add,
                                            doAction = { chooser.launch("apk", "jar") },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                    PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(stringResource(title)) },
                            )
                        }
                    }
                    HorizontalDivider()
                }
            },
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(padding),
                beyondViewportPageCount = 1,
            ) { page ->
                when (page) {
                    0 -> SourceHomeScreenContent(
                        onSourceClick = { navigator push SourceScreen(it) },
                        onLatestClick = { navigator push SourceScreen(it, startLatest = true) },
                        onTogglePinned = sourcesVm::togglePinned,
                        isLoading = sourcesVm.isLoading.collectAsState().value,
                        sources = sourcesVm.sources.collectAsState().value,
                    )

                    else -> ExtensionsScreenContent(
                        extensions = extensionsVm.extensions.collectAsState().value,
                        isLoading = extensionsVm.isLoading.collectAsState().value,
                        installExtension = extensionsVm::install,
                        updateExtension = extensionsVm::update,
                        uninstallExtension = extensionsVm::uninstall,
                    )
                }
            }
        }

        LanguageDialog(
            sourcesLanguageDialog,
            sourcesVm.languages.collectAsState().value,
            sourcesVm.sourceLanguages.collectAsState().value,
            sourcesVm::setEnabledLanguages,
        )
        LanguageDialog(
            extensionsLanguageDialog,
            extensionsVm.enabledLangs.collectAsState().value,
            extensionsVm.availableLangs.collectAsState().value,
            extensionsVm::setEnabledLanguages,
        )
    }
}
