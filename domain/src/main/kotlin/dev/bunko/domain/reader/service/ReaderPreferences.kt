/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.reader.service

import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.PreferenceStore
import dev.bunko.domain.reader.model.DefaultReaderMode
import dev.bunko.domain.reader.model.ImageSaveTarget
import dev.bunko.domain.reader.model.ReaderBackground
import dev.bunko.domain.reader.model.TappingInvertMode
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

class ReaderPreferences(
    private val preferenceStore: PreferenceStore,
    private val factory: (String) -> PreferenceStore,
) {
    fun preload(): Preference<Int> = preferenceStore.getInt("preload", 6)

    fun threads(): Preference<Int> = preferenceStore.getInt("threads", 4)

    fun volumeKeys(): Preference<Boolean> = preferenceStore.getBoolean("volume_keys", true)

    fun volumeKeysInverted(): Preference<Boolean> = preferenceStore.getBoolean("volume_keys_inverted", false)

    fun keepScreenOn(): Preference<Boolean> = preferenceStore.getBoolean("keep_screen_on", true)

    fun tappingInvertMode(): Preference<TappingInvertMode> =
        preferenceStore.getJsonObject("tapping_invert_mode", TappingInvertMode.NONE, TappingInvertMode.serializer())

    fun dimmer(): Preference<Float> = preferenceStore.getFloat("dimmer", 0f)

    fun background(): Preference<ReaderBackground> =
        preferenceStore.getJsonObject("background", ReaderBackground.Black, ReaderBackground.serializer())

    fun autoScrollSpeed(): Preference<Int> = preferenceStore.getInt("auto_scroll_speed", 60)

    fun imageSaveTarget(): Preference<ImageSaveTarget> =
        preferenceStore.getJsonObject("image_save_target", ImageSaveTarget.Ask, ImageSaveTarget.serializer())

    fun imageSaveFolder(): Preference<String> = preferenceStore.getString("image_save_folder", "")

    fun modes(): Preference<List<String>> =
        preferenceStore.getJsonObject(
            "modes",
            DefaultReaderMode.entries.map { it.res },
            ListSerializer(String.serializer()),
        )

    fun mode(): Preference<String> = preferenceStore.getString("mode", "RTL")

    fun getMode(mode: String): ReaderModePreferences = ReaderModePreferences(mode, factory)
}
