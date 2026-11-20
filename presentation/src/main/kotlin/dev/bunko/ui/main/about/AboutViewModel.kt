/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.about

import dev.bunko.ui.base.userMessage
import dev.bunko.core.util.DateHandler
import dev.bunko.domain.settings.interactor.AboutServer
import dev.bunko.domain.settings.model.About
import dev.bunko.domain.updates.interactor.UpdateChecker
import dev.bunko.domain.updates.interactor.UpdateChecker.Update
import dev.bunko.i18n.MR
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Instant

@Inject
class AboutViewModel(
    private val dateHandler: DateHandler,
    private val aboutServer: AboutServer,
    private val updateChecker: UpdateChecker,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    private val _aboutHolder = MutableStateFlow<About?>(null)
    val aboutHolder = _aboutHolder.asStateFlow()

    val formattedBuildTime = aboutHolder.map { about ->
        about ?: return@map ""
        getFormattedDate(Instant.fromEpochSeconds(about.buildTime))
    }.stateIn(scope, SharingStarted.Eagerly, "")

    private val _updates = MutableSharedFlow<Update.UpdateFound>()
    val updates = _updates.asSharedFlow()

    init {
        getAbout()
    }

    private fun getAbout() {
        scope.launch {
            _aboutHolder.value = aboutServer.await(onError = { toast(it.userMessage.toPlatformString()) })
        }
    }

    fun checkForUpdates() {
        scope.launch {
            toast(MR.strings.update_check_look_for_updates.toPlatformString())
            when (val update = updateChecker.await(true, onError = { toast(it.userMessage.toPlatformString()) })) {
                is Update.UpdateFound -> _updates.emit(update)
                is Update.NoUpdatesFound -> toast(MR.strings.update_check_no_new_updates.toPlatformString())
                null -> Unit
            }
        }
    }

    private fun getFormattedDate(time: Instant): String = dateHandler.dateTimeFormat(time)

    companion object {
        private val log = logging()
    }
}
