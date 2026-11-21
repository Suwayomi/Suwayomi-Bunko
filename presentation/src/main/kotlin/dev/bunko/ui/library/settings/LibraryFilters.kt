/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.bunko.domain.library.model.FilterState
import dev.bunko.i18n.MR
import dev.bunko.ui.base.sheet.TriStateItem
import dev.bunko.uicore.resources.stringResource

@Composable
fun getLibraryFilters(vm: LibrarySettingsViewModel): @Composable () -> Unit =
    remember(vm) {
        @Composable {
            LibraryFilters(
                downloaded = vm.filterDownloaded.collectAsState().value,
                unread = vm.filterUnread.collectAsState().value,
                completed = vm.filterCompleted.collectAsState().value,
                started = vm.filterStarted.collectAsState().value,
                bookmarked = vm.filterBookmarked.collectAsState().value,
                setDownloadedFilter = { vm.filterDownloaded.value = it },
                setUnreadFilter = { vm.filterUnread.value = it },
                setCompletedFilter = { vm.filterCompleted.value = it },
                setStartedFilter = { vm.filterStarted.value = it },
                setBookmarkedFilter = { vm.filterBookmarked.value = it },
            )
        }
    }

@Composable
fun LibraryFilters(
    downloaded: FilterState,
    unread: FilterState,
    completed: FilterState,
    started: FilterState,
    bookmarked: FilterState,
    setDownloadedFilter: (FilterState) -> Unit,
    setUnreadFilter: (FilterState) -> Unit,
    setCompletedFilter: (FilterState) -> Unit,
    setStartedFilter: (FilterState) -> Unit,
    setBookmarkedFilter: (FilterState) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        TriStateItem(stringResource(MR.strings.filter_downloaded), downloaded, setDownloadedFilter)
        TriStateItem(stringResource(MR.strings.filter_unread), unread, setUnreadFilter)
        TriStateItem(stringResource(MR.strings.filter_started), started, setStartedFilter)
        TriStateItem(stringResource(MR.strings.filter_bookmarked), bookmarked, setBookmarkedFilter)
        TriStateItem(stringResource(MR.strings.filter_completed), completed, setCompletedFilter)
    }
}
