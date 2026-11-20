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
import androidx.compose.ui.util.fastForEach
import dev.bunko.domain.library.model.Sort
import dev.bunko.ui.base.sheet.SortItem
import dev.bunko.uicore.resources.stringResource

@Composable
fun getLibrarySort(vm: LibrarySettingsViewModel): @Composable () -> Unit =
    remember(vm) {
        @Composable {
            LibrarySort(
                mode = vm.sortMode.collectAsState().value,
                ascending = vm.sortAscending.collectAsState().value,
                setMode = {
                    vm.sortMode.value = it
                    vm.sortAscending.value = true
                },
                setAscending = { vm.sortAscending.value = it },
            )
        }
    }

@Composable
fun LibrarySort(
    mode: Sort,
    ascending: Boolean,
    setMode: (Sort) -> Unit,
    setAscending: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Sort.entries.fastForEach { sort ->
            SortItem(
                label = stringResource(sort.res),
                descending = (!ascending).takeIf { mode == sort },
                onClick = { if (mode == sort) setAscending(!ascending) else setMode(sort) },
            )
        }
    }
}
