/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class Activity(
    val name: String,
    val type: Int,
    val details: String? = null,
    val state: String? = null,
    val timestamps: Timestamps? = null,
    val assets: Assets? = null,
    @SerialName("application_id")
    val applicationId: String? = null,
) {
    @Serializable
    data class Timestamps(
        val start: Long? = null,
    )

    @Serializable
    data class Assets(
        @SerialName("large_image")
        val largeImage: String? = null,
        @SerialName("large_text")
        val largeText: String? = null,
    )

    companion object {
        const val TYPE_WATCHING = 3
    }
}

@Serializable
data class Presence(
    val activities: List<Activity>,
    val status: String,
    val afk: Boolean,
    val since: Long? = null,
)

@Serializable
internal data class Identify(
    val token: String,
    val properties: Properties,
    val compress: Boolean = false,
    val intents: Int = 0,
    val presence: Presence? = null,
) {
    @Serializable
    data class Properties(
        val os: String = "Android",
        val browser: String = "Discord Android",
        val device: String = "Android",
    )
}

@Serializable
internal data class GatewayMessage(
    val op: Int,
    val d: JsonElement? = null,
    val s: Int? = null,
    val t: String? = null,
)

@Serializable
internal data class OutgoingMessage<T>(
    val op: Int,
    val d: T,
)

@Serializable
internal data class DiscordUser(
    val username: String,
    @SerialName("global_name")
    val globalName: String? = null,
)

@Serializable
internal data class ExternalAsset(
    @SerialName("external_asset_path")
    val externalAssetPath: String? = null,
)

internal object OpCode {
    const val DISPATCH = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val PRESENCE_UPDATE = 3
    const val RESUME = 6
    const val RECONNECT = 7
    const val INVALID_SESSION = 9
    const val HELLO = 10
    const val HEARTBEAT_ACK = 11
}
