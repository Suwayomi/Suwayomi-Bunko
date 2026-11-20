/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main

import dev.bunko.ui.main.components.SyncStatusChip
import dev.bunko.ui.reader.SyncStatus
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.bunko.ui.base.LocalViewModels
import androidx.activity.compose.BackHandler
import dev.bunko.ui.main.components.WithBottomNav
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun MainMenu() {
    val viewModels = LocalViewModels.current
    val vm = remember { viewModels.mainViewModel() }
    DisposableEffect(vm) {
        onDispose(vm::onDispose)
    }
    val confirmExit by vm.confirmExit.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()
    Surface {
        Navigator(vm.startScreen.toScreen()) { navigator ->
            WithBottomNav(navigator) {
                MainWindow(navigator, syncStatus, Modifier)
            }
            BackHandler(navigator.size == 1 && navigator.lastItem::class != vm.startScreen.toScreenClazz()) {
                navigator replaceAll vm.startScreen.toScreen()
            }
            var exitConfirmed by remember { mutableStateOf(false) }
            LaunchedEffect(exitConfirmed) {
                delay(2.seconds)
                exitConfirmed = false
            }
            BackHandler(
                confirmExit &&
                    navigator.size == 1 &&
                    navigator.lastItem::class == vm.startScreen.toScreenClazz() &&
                    !exitConfirmed,
            ) {
                exitConfirmed = true
                vm.confirmExitToast()
            }
        }
    }
}

@Composable
fun MainWindow(
    navigator: Navigator,
    syncStatus: SyncStatus,
    modifier: Modifier,
) {
    Surface(Modifier.fillMaxSize() then modifier) {
        Box {
            FadeTransition(navigator)
            SyncStatusChip(syncStatus, Modifier.align(Alignment.BottomCenter))
        }
    }
}
