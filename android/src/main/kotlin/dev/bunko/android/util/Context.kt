/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android.util

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService

val Context.notificationManager: NotificationManager
    get() = getSystemService()!!

fun Context.notificationBuilder(
    channelId: String,
    block: (NotificationCompat.Builder.() -> Unit)? = null,
): NotificationCompat.Builder {
    val builder = NotificationCompat.Builder(this, channelId)
    if (block != null) {
        builder.block()
    }
    return builder
}

fun Context.notification(
    channelId: String,
    block: (NotificationCompat.Builder.() -> Unit)?,
): Notification {
    val builder = notificationBuilder(channelId, block)
    return builder.build()
}
