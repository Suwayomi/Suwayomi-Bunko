/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.components

import dev.bunko.domain.base.ServerSubscriptions
import dev.bunko.domain.library.service.LibraryUpdateService
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class LibraryUpdatesViewModel(
    private val serverSubscriptions: ServerSubscriptions,
    contextWrapper: ContextWrapper,
    @Assisted standalone: Boolean,
) : ViewModel(contextWrapper) {
    private val uiScope = if (standalone) {
        MainScope()
    } else {
        null
    }

    override val scope: CoroutineScope
        get() = uiScope ?: super.scope

    val serviceStatus = LibraryUpdateService.status.asStateFlow()
    val jobsInfo = LibraryUpdateService.jobsInfo.asStateFlow()

    fun restartLibraryUpdates() = serverSubscriptions.restartLibraryUpdates()

    override fun onDispose() {
        super.onDispose()
        uiScope?.cancel()
    }

    private companion object {
        private val log = logging()
    }
}
