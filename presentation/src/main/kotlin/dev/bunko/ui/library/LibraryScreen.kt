/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.collectAsState
import dev.bunko.domain.library.model.FilterState
import dev.bunko.ui.base.screen.BaseScreen
import dev.bunko.ui.library.components.LibraryScreenContent
import dev.bunko.ui.library.settings.getLibraryDisplay
import dev.bunko.ui.library.settings.getLibraryFilters
import dev.bunko.ui.library.settings.getLibrarySort
import dev.bunko.ui.manga.MangaScreen
import dev.bunko.ui.reader.rememberReaderLauncher
import dev.bunko.ui.sources.globalsearch.GlobalSearchScreen
import dev.bunko.ui.stateViewModel
import dev.bunko.ui.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class LibraryScreen : BaseScreen() {
    @Composable
    override fun Content() {
        val vm = stateViewModel { libraryViewModel(it) }
        val settingsVM = viewModel { librarySettingsViewModel() }
        val updatesVM = viewModel { libraryUpdatesViewModel(false) }
        val navigator = LocalNavigator.currentOrThrow
        val readerLauncher = rememberReaderLauncher()
        val hasActiveFilters = listOf(
            settingsVM.filterDownloaded,
            settingsVM.filterUnread,
            settingsVM.filterCompleted,
            settingsVM.filterStarted,
            settingsVM.filterBookmarked,
        ).any { it.collectAsState().value != FilterState.IGNORED }
        LibraryScreenContent(
            libraryState = vm.categories.collectAsState().value,
            selectedCategoryIndex = vm.selectedCategoryIndex.collectAsState().value,
            displayMode = vm.displayMode.collectAsState().value,
            gridColumns = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                vm.gridColumnsLandscape.collectAsState().value
            } else {
                vm.gridColumns.collectAsState().value
            },
            gridSize = vm.gridSize.collectAsState().value,
            query = vm.query.collectAsState().value,
            updateQuery = vm::updateQuery,
            onDeviceOnly = vm.onDeviceOnly.collectAsState().value,
            toggleOnDeviceOnly = vm::toggleOnDeviceOnly,
            getLibraryForPage = { vm.getLibraryForCategoryId(it).collectAsState() },
            onPageChanged = vm::setSelectedPage,
            resume = vm.resume.collectAsState().value,
            onResume = readerLauncher::launch,
            onClickManga = { navigator push MangaScreen(it) },
            onRemoveMangaClicked = vm::removeManga,
            onDeleteDownloadsClicked = vm::deleteDownloads,
            onSetRead = vm::setRead,
            onDownloadUnread = vm::downloadUnread,
            onSetCategories = vm::setCategories,
            editableCategories = vm.editableCategories.collectAsState().value,
            isRefreshing = vm.isRefreshing.collectAsState().value,
            onRefreshCategory = vm::refreshCategory,
            onGlobalSearch = { navigator push GlobalSearchScreen(it) },
            onUpdateLibrary = vm::updateLibrary,
            showingMenu = vm.showingMenu.collectAsState().value,
            setShowingMenu = vm::setShowingMenu,
            libraryFilters = getLibraryFilters(settingsVM),
            librarySort = getLibrarySort(settingsVM),
            libraryDisplay = getLibraryDisplay(settingsVM),
            hasActiveFilters = hasActiveFilters,
            showUnread = vm.unreadBadges.collectAsState().value,
            showDownloaded = vm.downloadBadges.collectAsState().value,
            showLanguage = vm.languageBadges.collectAsState().value,
            showLocal = vm.localBadges.collectAsState().value,
            updateWebsocketStatus = updatesVM.serviceStatus.collectAsState().value,
            restartLibraryUpdates = updatesVM::restartLibraryUpdates,
        )
    }
}
