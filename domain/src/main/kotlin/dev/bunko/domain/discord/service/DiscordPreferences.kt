/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord.service

import dev.bunko.core.prefs.EncryptedPreference
import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.PreferenceStore
import dev.bunko.domain.discord.model.DiscordStatus

class DiscordPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun enabled(): Preference<Boolean> = preferenceStore.getBoolean("enabled", false)

    fun token(): Preference<String> = EncryptedPreference(preferenceStore.getString("token", ""))

    fun dropStaleLink() {
        if (username().get().isNotEmpty() && token().get().isEmpty()) unlink()
    }

    fun unlink() {
        enabled().set(false)
        token().delete()
        username().delete()
    }

    fun username(): Preference<String> = preferenceStore.getString("username", "")

    fun status(): Preference<DiscordStatus> =
        preferenceStore.getJsonObject("status", DiscordStatus.Online, DiscordStatus.serializer())

    fun showChapter(): Preference<Boolean> = preferenceStore.getBoolean("show_chapter", true)

    fun showCover(): Preference<Boolean> = preferenceStore.getBoolean("show_cover", true)

    fun showTimestamp(): Preference<Boolean> = preferenceStore.getBoolean("show_timestamp", true)
}
