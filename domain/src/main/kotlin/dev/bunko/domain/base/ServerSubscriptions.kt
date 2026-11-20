/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.base

import dev.bunko.core.di.AppScope
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.core.prefs.getAsFlow
import dev.bunko.domain.download.service.DownloadService
import dev.bunko.domain.library.service.LibraryUpdateService
import dev.bunko.domain.server.service.ServerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

@AppScope
@Inject
class ServerSubscriptions(
    private val serverPreferences: ServerPreferences,
    private val downloadService: DownloadService,
    private val libraryUpdateService: LibraryUpdateService,
) {
    private var scope: CoroutineScope? = null
    private var downloads: Job? = null
    private var libraryUpdates: Job? = null

    fun start(scope: CoroutineScope) {
        this.scope = scope + Dispatchers.IO
        restartDownloads()
        restartLibraryUpdates()
    }

    fun restartDownloads() {
        downloads?.cancel()
        downloads = keepSubscribed(downloadService::getSubscription)
    }

    fun restartLibraryUpdates() {
        libraryUpdates?.cancel()
        libraryUpdates = keepSubscribed(libraryUpdateService::getSubscription)
    }

    private fun keepSubscribed(subscription: () -> Flow<*>): Job? {
        val scope = scope ?: return null
        return serverPreferences.serverUrl().getAsFlow()
            .mapLatest {
                while (true) {
                    runCatching { subscription().collect() }.throwIfCancellation()
                    delay(RESUBSCRIBE_DELAY)
                }
            }
            .launchIn(scope)
    }

    private companion object {
        val RESUBSCRIBE_DELAY = 5.seconds
    }
}
