/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.settings

import androidx.compose.runtime.Composable
import dev.bunko.i18n.MR
import dev.bunko.ui.base.sheet.TabbedSheet
import kotlinx.collections.immutable.persistentListOf

private val LibrarySheetTabs = persistentListOf(
    MR.strings.action_filter,
    MR.strings.library_sort,
    MR.strings.library_display,
)

@Composable
fun LibrarySheet(
    libraryFilters: @Composable () -> Unit,
    librarySort: @Composable () -> Unit,
    libraryDisplay: @Composable () -> Unit,
) {
    TabbedSheet(LibrarySheetTabs) { page ->
        when (page) {
            0 -> libraryFilters()
            1 -> librarySort()
            else -> libraryDisplay()
        }
    }
}
