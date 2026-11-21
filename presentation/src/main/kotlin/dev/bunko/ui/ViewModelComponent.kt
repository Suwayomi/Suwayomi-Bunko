/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui

import dev.bunko.ui.settings.tracking.SettingsTrackingViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.bunko.ui.base.LocalViewModels
import dev.bunko.ui.base.state.addScreenModelKey
import dev.bunko.ui.base.state.createSavedStateHandle
import dev.bunko.ui.base.theme.AppThemeViewModel
import dev.bunko.ui.categories.CategoriesScreenViewModel
import dev.bunko.ui.downloads.DownloadsScreenViewModel
import dev.bunko.ui.extensions.ExtensionsScreenViewModel
import dev.bunko.ui.library.LibraryScreenViewModel
import dev.bunko.ui.library.settings.LibrarySettingsViewModel
import dev.bunko.ui.main.MainViewModel
import dev.bunko.ui.main.about.AboutViewModel
import dev.bunko.ui.main.more.MoreViewModel
import dev.bunko.ui.main.components.LibraryUpdatesViewModel
import dev.bunko.ui.manga.MangaScreenViewModel
import dev.bunko.ui.offline.OfflineScreenViewModel
import dev.bunko.ui.reader.ReaderMenuViewModel
import dev.bunko.ui.settings.SettingsAdvancedViewModel
import dev.bunko.ui.settings.SettingsBackupViewModel
import dev.bunko.ui.settings.SettingsGeneralViewModel
import dev.bunko.ui.settings.SettingsLibraryViewModel
import dev.bunko.ui.settings.SettingsReaderViewModel
import dev.bunko.ui.settings.SettingsServerViewModel
import dev.bunko.ui.settings.ThemesViewModel
import dev.bunko.ui.sources.browse.SourceScreenViewModel
import dev.bunko.ui.sources.browse.filter.SourceFiltersViewModel
import dev.bunko.ui.sources.globalsearch.GlobalSearchViewModel
import dev.bunko.ui.sources.home.SourceHomeScreenViewModel
import dev.bunko.ui.sources.settings.SourceSettingsScreenViewModel
import dev.bunko.ui.statistics.StatisticsScreenViewModel
import dev.bunko.ui.updates.UpdatesScreenViewModel
import dev.bunko.ui.history.ReadHistoryViewModel
import dev.bunko.ui.schedule.ScheduleViewModel
import dev.bunko.uicore.vm.ViewModel
import cafe.adriel.voyager.androidx.AndroidScreenLifecycleOwner
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen

interface ViewModelComponent {
    val aboutViewModel: () -> AboutViewModel
    val appThemeViewModel: () -> AppThemeViewModel
    val categoryViewModel: () -> CategoriesScreenViewModel
    val downloadsViewModel: (Boolean) -> DownloadsScreenViewModel
    val offlineViewModel: () -> OfflineScreenViewModel
    val statisticsViewModel: () -> StatisticsScreenViewModel
    val extensionsViewModel: () -> ExtensionsScreenViewModel
    val libraryViewModel: (SavedStateHandle) -> LibraryScreenViewModel
    val librarySettingsViewModel: () -> LibrarySettingsViewModel
    val libraryUpdatesViewModel: (Boolean) -> LibraryUpdatesViewModel
    val mainViewModel: () -> MainViewModel
    val moreViewModel: () -> MoreViewModel
    val mangaViewModel: (params: MangaScreenViewModel.Params) -> MangaScreenViewModel
    val readerViewModel: (params: ReaderMenuViewModel.Params) -> ReaderMenuViewModel
    val settingsAdvancedViewModel: () -> SettingsAdvancedViewModel
    val themesViewModel: () -> ThemesViewModel
    val settingsBackupViewModel: () -> SettingsBackupViewModel
    val settingsGeneralViewModel: () -> SettingsGeneralViewModel
    val settingsLibraryViewModel: () -> SettingsLibraryViewModel
    val settingsReaderViewModel: () -> SettingsReaderViewModel
    val settingsServerViewModel: () -> SettingsServerViewModel
    val settingsTrackingViewModel: () -> SettingsTrackingViewModel
    val sourceFiltersViewModel: (SavedStateHandle, params: SourceFiltersViewModel.Params) -> SourceFiltersViewModel
    val sourceSettingsViewModel: (params: SourceSettingsScreenViewModel.Params) -> SourceSettingsScreenViewModel
    val sourceHomeViewModel: (SavedStateHandle) -> SourceHomeScreenViewModel
    val globalSearchViewModel: (SavedStateHandle, params: GlobalSearchViewModel.Params) -> GlobalSearchViewModel
    val sourceViewModel: (SavedStateHandle, params: SourceScreenViewModel.Params) -> SourceScreenViewModel
    val updatesViewModel: () -> UpdatesScreenViewModel
    val readHistoryViewModel: () -> ReadHistoryViewModel
    val scheduleViewModel: () -> ScheduleViewModel
}

@Composable
inline fun <reified VM : ViewModel> Screen.viewModel(
    tag: String? = null,
    crossinline factory: @DisallowComposableCalls ViewModelComponent.() -> VM,
): VM {
    val viewModelFactory = LocalViewModels.current
    return rememberScreenModel(tag) { viewModelFactory.factory() }
}

@Composable
inline fun <reified VM : ViewModel> Screen.stateViewModel(
    tag: String? = null,
    crossinline factory: @DisallowComposableCalls ViewModelComponent.(SavedStateHandle) -> VM,
): VM {
    val viewModelFactory = LocalViewModels.current
    val lifecycle = LocalLifecycleOwner.current as AndroidScreenLifecycleOwner
    val handle = remember {
        lifecycle.defaultViewModelCreationExtras
            .addScreenModelKey<VM>(this, tag)
            .createSavedStateHandle()
    }
    return rememberScreenModel(tag) { viewModelFactory.factory(handle) }
}
