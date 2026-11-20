/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android.data.download

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
import dev.bunko.core.lang.chop
import dev.bunko.domain.download.model.DownloadQueueItem
import dev.bunko.domain.download.model.DownloadState
import dev.bunko.domain.download.service.DownloadService
import dev.bunko.i18n.MR
import com.diamondedge.logging.logging
import dev.icerock.moko.resources.desc.desc
import dev.icerock.moko.resources.format
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AndroidDownloadService : Service() {
    private val scope = MainScope()

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_DOWNLOADER_DOWNLOADING, progressNotification(DownloadService.downloadQueue.value))
        val progress = DownloadService.downloadQueue
            .onEach { notificationManager.notify(Notifications.ID_DOWNLOADER_DOWNLOADING, progressNotification(it)) }
            .launchIn(scope)
        scope.launch {
            DownloadService.isDownloading.first { !it }
            progress.cancel()
            ServiceCompat.stopForeground(this@AndroidDownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
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
        notificationBuilder(Notifications.CHANNEL_DOWNLOADER_DOWNLOADING) {
            setSmallIcon(R.drawable.ic_round_get_app_24)
            setOnlyAlertOnce(true)
            setOngoing(true)
        }
    }

    private fun progressNotification(queue: List<DownloadQueueItem>) =
        with(progressBuilder) {
            val item = queue.lastOrNull { it.state == DownloadState.DOWNLOADING }
            if (item == null) {
                setContentTitle(MR.strings.downloader_running.desc().toString(this@AndroidDownloadService))
                setContentText(null)
                setProgress(0, 0, true)
            } else {
                val max = item.chapter.pageCount
                val current = (max * item.progress).toInt().coerceIn(0, max)
                val title = item.manga.title.chop(15)
                val chapter = item.chapter.name.replaceFirst(
                    "${Pattern.quote(title)}[\\s]*[-]*[\\s]*".toRegex(RegexOption.IGNORE_CASE),
                    "",
                )
                setContentTitle("$title - $chapter".chop(30))
                setContentText(MR.strings.chapter_downloading_progress.format(current, max).toString(this@AndroidDownloadService))
                setProgress(max, current, false)
            }
            build()
        }

    companion object {
        private val log = logging()

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, AndroidDownloadService::class.java)) }
                .onFailure { log.warn(it) { "Download service not started; progress shows in the app only" } }
        }
    }
}
