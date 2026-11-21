/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.bunko.ui.base.screen.BaseScreen
import dev.bunko.ui.manga.MangaScreen
import dev.bunko.ui.sources.browse.SourceScreen
import dev.bunko.ui.sources.browse.components.rememberLibraryToggle
import dev.bunko.ui.sources.globalsearch.components.GlobalSearchScreenContent
import dev.bunko.ui.stateViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class GlobalSearchScreen(
    private val initialQuery: String,
) : BaseScreen() {
    @Composable
    override fun Content() {
        val vm = stateViewModel {
            globalSearchViewModel(it, GlobalSearchViewModel.Params(initialQuery))
        }
        val navigator = LocalNavigator.currentOrThrow
        val onMangaLongClick = rememberLibraryToggle(
            categories = vm.categories.collectAsState().value,
            onAdd = vm::addToLibrary,
            onRemove = vm::removeFromLibrary,
        )

        GlobalSearchScreenContent(
            sources = vm.sources.collectAsState().value,
            isLoading = vm.isLoading.collectAsState().value,
            results = vm.results,
            query = vm.query.collectAsState().value,
            pinnedOnly = vm.pinnedOnly.collectAsState().value,
            setPinnedOnly = vm::setPinnedOnly,
            setQuery = vm::setQuery,
            submitSearch = vm::startSearch,
            onSourceClick = { navigator push SourceScreen(it, vm.query.value) },
            onMangaClick = {
                navigator push MangaScreen(it.id)
            },
            onMangaLongClick = onMangaLongClick,
        )
    }
}
