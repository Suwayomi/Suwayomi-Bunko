/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android.data.library

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.bunko.android.R
import dev.bunko.android.data.notification.Notifications
import dev.bunko.android.util.notificationBuilder
import dev.bunko.android.util.notificationManager
import dev.bunko.domain.library.model.UpdaterJobsInfo
import dev.bunko.domain.library.service.LibraryUpdateService
import dev.bunko.i18n.MR
import com.diamondedge.logging.logging
import dev.icerock.moko.resources.format
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AndroidLibraryService : Service() {
    private val scope = MainScope()

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_LIBRARY_PROGRESS, progressNotification(LibraryUpdateService.jobsInfo.value))
        val progress = LibraryUpdateService.jobsInfo
            .onEach { if (it.isRunning) notificationManager.notify(Notifications.ID_LIBRARY_PROGRESS, progressNotification(it)) }
            .launchIn(scope)
        scope.launch {
            LibraryUpdateService.isUpdating.first { !it }
            progress.cancel()
            ServiceCompat.stopForeground(this@AndroidLibraryService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_NOT_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private val progressBuilder by lazy {
        notificationBuilder(Notifications.CHANNEL_LIBRARY_PROGRESS) {
            setSmallIcon(R.drawable.ic_round_get_app_24)
            setOnlyAlertOnce(true)
            setOngoing(true)
        }
    }

    private fun progressNotification(jobs: UpdaterJobsInfo) =
        progressBuilder
            .setContentTitle(MR.strings.notification_updating.format(jobs.finishedJobs, jobs.totalJobs).toString(this))
            .setProgress(jobs.totalJobs, jobs.finishedJobs, false)
            .build()

    companion object {
        private val log = logging()

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, AndroidLibraryService::class.java)) }
                .onFailure { log.warn(it) { "Library update service not started; progress shows in the app only" } }
        }
    }
}
