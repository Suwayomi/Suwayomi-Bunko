/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.diamondedge.logging.logging
import dev.bunko.android.AppComponent
import dev.bunko.android.data.notification.Notifications
import dev.bunko.android.util.notificationBuilder
import dev.bunko.android.util.notificationManager
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.updates.interactor.UpdateChecker.Update
import dev.bunko.i18n.MR
import dev.icerock.moko.resources.desc.desc
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result =
        try {
            val update = AppComponent.getInstance(context.applicationContext)
                .let {
                    if (it.updatePreferences.enabled().get()) {
                        it.updateChecker.await(false)
                    } else {
                        null
                    }
                }

            if (update is Update.UpdateFound) {
                val openRelease = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(Intent.ACTION_VIEW, update.release.releaseLink.toUri()),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                context.notificationBuilder(Notifications.CHANNEL_APP_UPDATE) {
                    setContentTitle(MR.strings.new_update_title.desc().toString(context))
                    setContentText(update.release.version)
                    setSmallIcon(android.R.drawable.stat_sys_download_done)
                    setContentIntent(openRelease)
                    setAutoCancel(true)
                }.let {
                    context.notificationManager.notify(Notifications.ID_UPDATES_TO_APP, it.build())
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.throwIfCancellation()
            log.warn(e) { "Update check failed" }
            Result.failure()
        }

    companion object {
        private const val TAG = "UpdateChecker"
        private val log = logging()

        fun setupTask(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                7,
                TimeUnit.DAYS,
                3,
                TimeUnit.HOURS,
            )
                .addTag(TAG)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
