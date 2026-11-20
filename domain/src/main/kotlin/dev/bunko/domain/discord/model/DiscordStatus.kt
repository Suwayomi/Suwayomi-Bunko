/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord.model

import dev.bunko.i18n.MR
import dev.icerock.moko.resources.StringResource
import kotlinx.serialization.Serializable

@Serializable
enum class DiscordStatus(
    val wire: String,
    val res: StringResource,
) {
    Online("online", MR.strings.discord_status_online),
    Idle("idle", MR.strings.discord_status_idle),
    Dnd("dnd", MR.strings.discord_status_dnd),
}

data class ReadingActivity(
    val mangaId: Long,
    val mangaTitle: String,
    val chapterName: String,
    val chapterNumber: Float,
    val startedAt: Long,
)
