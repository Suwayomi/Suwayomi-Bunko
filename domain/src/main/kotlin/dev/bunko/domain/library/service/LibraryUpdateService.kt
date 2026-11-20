/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.library.service

import dev.bunko.domain.ServerListeners
import dev.bunko.domain.base.WebsocketService
import dev.bunko.domain.library.model.UpdaterJobsInfo
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Inject

@Inject
class LibraryUpdateService(
    private val libraryUpdateRepository: LibraryRepository,
    private val serverListeners: ServerListeners,
) {
    fun getSubscription() = libraryUpdateRepository.libraryUpdateSubscription()
        .onStart {
            log.info { "Starting library update status subscription" }
            status.value = WebsocketService.Status.STARTING
        }
        .catch { error ->
            log.error(error) { "Error in library update status subscription" }
            status.value = WebsocketService.Status.STOPPED
        }
        .map { info ->
            status.value = WebsocketService.Status.RUNNING
            val previous = jobsInfo.getAndUpdate { info }
            if (previous.isRunning && !info.isRunning) {
                log.info { "Library update finished, reloading library data" }
                serverListeners.updateLibrary()
            }
        }

    companion object {
        private val log = logging()

        val status = MutableStateFlow(WebsocketService.Status.STARTING)
        val jobsInfo = MutableStateFlow(
            UpdaterJobsInfo(
                finishedJobs = 0,
                isRunning = false,
                skippedCategoriesCount = 0,
                skippedMangasCount = 0,
                totalJobs = 0,
            ),
        )

        val isUpdating = jobsInfo.map { it.isRunning }.distinctUntilChanged()
    }
}
