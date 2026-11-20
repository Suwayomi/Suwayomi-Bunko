/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.updates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.bunko.ui.base.offline.OfflineState
import dev.bunko.ui.downloads.DownloadsScreen
import dev.bunko.ui.manga.MangaScreen
import dev.bunko.ui.reader.rememberReaderLauncher
import dev.bunko.ui.updates.components.UpdatesScreenContent
import dev.bunko.ui.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

@Composable
fun Screen.UpdatesTab(tabs: @Composable () -> Unit) {
    val vm = viewModel { updatesViewModel() }
    val updatesVM = viewModel { libraryUpdatesViewModel(false) }
    val navigator = LocalNavigator.currentOrThrow
    val readerLauncher = rememberReaderLauncher()
    val offline by vm.offlineStates.collectAsState()
    UpdatesScreenContent(
        tabs = tabs,
        downloadQueue = vm.downloadQueue.collectAsState().value,
        deviceSaving = vm.deviceSaving.collectAsState().value,
        onDevice = { offline[it] == OfflineState.Downloaded },
        deleteDeviceCopy = vm::deleteDeviceCopy,
        openQueue = { navigator push DownloadsScreen() },
        isLoading = vm.isLoading.collectAsState().value,
        updates = vm.updates.collectAsState().value,
        inActionMode = vm.inActionMode.collectAsState().value,
        selectedItems = vm.selectedItems.collectAsState().value,
        loadNextPage = vm::loadNextPage,
        openChapter = readerLauncher::launch,
        openManga = { navigator push MangaScreen(it) },
        markRead = vm::markRead,
        markUnread = vm::markUnread,
        bookmarkChapter = vm::bookmarkChapter,
        unBookmarkChapter = vm::unBookmarkChapter,
        downloadChapter = vm::downloadChapter,
        deleteDownloadedChapter = vm::deleteDownloadedChapter,
        stopDownloadingChapter = vm::stopDownloadingChapter,
        onSelectChapter = vm::selectChapter,
        onLongClickChapter = vm::longPressChapter,
        onUnselectChapter = vm::unselectChapter,
        selectAll = vm::selectAll,
        invertSelection = vm::invertSelection,
        clearSelection = vm::clearSelection,
        onUpdateLibrary = vm::updateLibrary,
        isRefreshing = vm.isRefreshing.collectAsState().value,
        onRefresh = vm::refresh,
        updateWebsocketStatus = updatesVM.serviceStatus.collectAsState().value,
        restartLibraryUpdates = updatesVM::restartLibraryUpdates,
    )
}
