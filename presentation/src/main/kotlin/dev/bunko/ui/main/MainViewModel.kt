/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main

import dev.bunko.ui.reader.ReaderProgressSync
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.i18n.MR
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import me.tatarka.inject.annotations.Inject

@Inject
class MainViewModel(
    uiPreferences: UiPreferences,
    progressSync: ReaderProgressSync,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    override val scope = MainScope()

    val startScreen = uiPreferences.startScreen().get()
    val syncStatus = progressSync.status
    val confirmExit = uiPreferences.confirmExit().stateIn(scope)

    override fun onDispose() {
        super.onDispose()
        scope.cancel()
    }

    fun confirmExitToast() {
        toast(MR.strings.confirm_exit_toast.toPlatformString())
    }
}
