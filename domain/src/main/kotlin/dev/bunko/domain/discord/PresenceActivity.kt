/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord

import dev.bunko.domain.discord.model.Activity
import dev.bunko.domain.discord.model.ReadingActivity

data class PresenceLabels(
    val appName: String,
    val browsing: String,
    val chapter: (String) -> String,
)

data class PresenceOptions(
    val showChapter: Boolean,
    val showTimestamp: Boolean,
)

const val APP_ICON_ASSET = "1573034120236368063"

fun presenceActivity(
    reading: ReadingActivity?,
    cover: String?,
    browsingSince: Long,
    labels: PresenceLabels,
    options: PresenceOptions,
    applicationId: String?,
): Activity =
    if (reading == null) {
        Activity(
            name = labels.appName,
            type = Activity.TYPE_WATCHING,
            details = labels.browsing,
            timestamps = Activity.Timestamps(browsingSince).takeIf { options.showTimestamp },
            assets = Activity.Assets(largeImage = APP_ICON_ASSET, largeText = labels.appName),
            applicationId = applicationId,
        )
    } else {
        Activity(
            name = reading.mangaTitle,
            type = Activity.TYPE_WATCHING,
            details = reading.mangaTitle,
            state = chapterLabel(reading, labels).takeIf { options.showChapter },
            timestamps = Activity.Timestamps(reading.startedAt).takeIf { options.showTimestamp },
            assets = Activity.Assets(largeImage = cover ?: APP_ICON_ASSET, largeText = reading.mangaTitle),
            applicationId = applicationId,
        )
    }

private fun chapterLabel(reading: ReadingActivity, labels: PresenceLabels): String {
    val number = reading.chapterNumber
    return when {
        number < 0f -> reading.chapterName
        number % 1f == 0f -> labels.chapter(number.toInt().toString())
        else -> labels.chapter(number.toString())
    }
}
