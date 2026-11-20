/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android.data.offline

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.bunko.android.AppComponent
import dev.bunko.android.R
import dev.bunko.android.data.notification.Notifications
import dev.bunko.android.util.notification
import dev.bunko.i18n.MR
import com.diamondedge.logging.logging
import dev.icerock.moko.resources.desc.desc
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OfflineDownloadService : Service() {
    private val scope = MainScope()

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_OFFLINE_RUNNING, runningNotification())
        val active = AppComponent.getInstance(applicationContext).offlineChapters.active
        scope.launch {
            active.first { !it }
            ServiceCompat.stopForeground(this@OfflineDownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
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

    private fun runningNotification() =
        notification(Notifications.CHANNEL_DOWNLOADER_RUNNING) {
            setContentTitle(MR.strings.offline_saving.desc().toString(this@OfflineDownloadService))
            setSmallIcon(R.drawable.ic_round_get_app_24)
            setOngoing(true)
        }

    companion object {
        private val log = logging()

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, OfflineDownloadService::class.java)) }
                .onFailure { log.warn(it) { "Offline download service not started; downloads stay in-process" } }
        }
    }
}
