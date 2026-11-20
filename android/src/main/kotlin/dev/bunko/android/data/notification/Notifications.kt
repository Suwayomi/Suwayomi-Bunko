/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android.data.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dev.bunko.android.util.buildNotificationChannel
import dev.bunko.android.util.buildNotificationChannelGroup
import dev.bunko.i18n.MR
import dev.icerock.moko.resources.desc.desc

object Notifications {
    private const val GROUP_DOWNLOADER = "group_downloader"
    const val CHANNEL_DOWNLOADER_RUNNING = "downloader_channel"
    const val CHANNEL_DOWNLOADER_DOWNLOADING = "downloader_channel_downloading"
    const val ID_DOWNLOADER_DOWNLOADING = -102
    const val ID_OFFLINE_RUNNING = -103

    private const val GROUP_LIBRARY = "group_library"
    const val CHANNEL_LIBRARY_PROGRESS = "library_progress_channel"
    const val ID_LIBRARY_PROGRESS = -302

    private const val GROUP_APK_UPDATES = "group_apk_updates"
    const val CHANNEL_APP_UPDATE = "app_apk_update_channel"
    const val ID_UPDATES_TO_APP = -201
    const val CHANNEL_EXTENSIONS_UPDATE = "ext_apk_update_channel"

    fun createChannels(context: Context) {
        val notificationService = NotificationManagerCompat.from(context)

        notificationService.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(GROUP_DOWNLOADER) {
                    setName(MR.strings.group_downloader.desc().toString(context))
                },
                buildNotificationChannelGroup(GROUP_LIBRARY) {
                    setName(MR.strings.group_library.desc().toString(context))
                },
                buildNotificationChannelGroup(GROUP_APK_UPDATES) {
                    setName(MR.strings.group_updates.desc().toString(context))
                },
            ),
        )

        notificationService.deleteNotificationChannel("library_updates_channel")

        notificationService.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(
                    CHANNEL_DOWNLOADER_RUNNING,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                ) {
                    setName(MR.strings.channel_active.desc().toString(context))
                    setGroup(GROUP_DOWNLOADER)
                    setShowBadge(false)
                },
                buildNotificationChannel(
                    CHANNEL_DOWNLOADER_DOWNLOADING,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                ) {
                    setName(MR.strings.channel_progress.desc().toString(context))
                    setGroup(GROUP_DOWNLOADER)
                    setShowBadge(false)
                },
                buildNotificationChannel(
                    CHANNEL_LIBRARY_PROGRESS,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                ) {
                    setName(MR.strings.channel_progress.desc().toString(context))
                    setGroup(GROUP_LIBRARY)
                    setShowBadge(false)
                },
                buildNotificationChannel(
                    CHANNEL_APP_UPDATE,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ) {
                    setGroup(GROUP_APK_UPDATES)
                    setName(MR.strings.group_updates_channel_app.desc().toString(context))
                },
                buildNotificationChannel(
                    CHANNEL_EXTENSIONS_UPDATE,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ) {
                    setGroup(GROUP_APK_UPDATES)
                    setName(MR.strings.group_updates_channel_ext.desc().toString(context))
                },
            ),
        )
    }
}
