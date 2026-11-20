/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.bunko.domain.library.model.DisplayMode
import dev.bunko.i18n.MR
import dev.bunko.ui.base.sheet.CheckboxItem
import dev.bunko.ui.base.sheet.SheetHeading
import dev.bunko.uicore.resources.stringResource

@Composable
fun getLibraryDisplay(vm: LibrarySettingsViewModel): @Composable () -> Unit =
    remember(vm) {
        @Composable {
            LibraryDisplay(
                displayMode = vm.displayMode.collectAsState().value,
                unreadBadges = vm.unreadBadges.collectAsState().value,
                downloadBadges = vm.downloadBadges.collectAsState().value,
                languageBadges = vm.languageBadges.collectAsState().value,
                localBadges = vm.localBadges.collectAsState().value,
                setDisplayMode = { vm.displayMode.value = it },
                setUnreadBadges = { vm.unreadBadges.value = it },
                setDownloadBadges = { vm.downloadBadges.value = it },
                setLanguageBadges = { vm.languageBadges.value = it },
                setLocalBadges = { vm.localBadges.value = it },
            )
        }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryDisplay(
    displayMode: DisplayMode,
    unreadBadges: Boolean,
    downloadBadges: Boolean,
    languageBadges: Boolean,
    localBadges: Boolean,
    setDisplayMode: (DisplayMode) -> Unit,
    setUnreadBadges: (Boolean) -> Unit,
    setDownloadBadges: (Boolean) -> Unit,
    setLanguageBadges: (Boolean) -> Unit,
    setLocalBadges: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SheetHeading(stringResource(MR.strings.display_mode))
        FlowRow(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DisplayMode.entries.fastForEach {
                FilterChip(
                    selected = it == displayMode,
                    onClick = { setDisplayMode(it) },
                    label = { Text(stringResource(it.res)) },
                )
            }
        }
        SheetHeading(stringResource(MR.strings.display_badges))
        CheckboxItem(stringResource(MR.strings.display_badge_downloaded), downloadBadges) { setDownloadBadges(!downloadBadges) }
        CheckboxItem(stringResource(MR.strings.display_badge_unread), unreadBadges) { setUnreadBadges(!unreadBadges) }
        CheckboxItem(stringResource(MR.strings.display_badge_language), languageBadges) { setLanguageBadges(!languageBadges) }
        CheckboxItem(stringResource(MR.strings.display_badge_local), localBadges) { setLocalBadges(!localBadges) }
    }
}
