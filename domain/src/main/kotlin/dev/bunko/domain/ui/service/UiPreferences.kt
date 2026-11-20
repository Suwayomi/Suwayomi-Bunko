/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.ui.service

import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.PreferenceStore
import dev.bunko.domain.ui.model.StartScreen
import dev.bunko.domain.ui.model.ThemeMode

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun themeMode(): Preference<ThemeMode> = preferenceStore.getJsonObject("theme_mode", ThemeMode.System, ThemeMode.serializer())

    fun lightTheme(): Preference<Int> = preferenceStore.getInt("theme_light", 0)

    fun darkTheme(): Preference<Int> = preferenceStore.getInt("theme_dark", 0)

    fun pureBlackDarkMode(): Preference<Boolean> = preferenceStore.getBoolean("pure_black_dark_mode", false)

    fun colorPrimaryLight(): Preference<Int> = preferenceStore.getInt("color_primary_light", 0)

    fun colorPrimaryDark(): Preference<Int> = preferenceStore.getInt("color_primary_dark", 0)

    fun colorSecondaryLight(): Preference<Int> = preferenceStore.getInt("color_secondary_light", 0)

    fun colorSecondaryDark(): Preference<Int> = preferenceStore.getInt("color_secondary_dark", 0)

    fun colorTertiaryLight(): Preference<Int> = preferenceStore.getInt("color_tertiary_light", 0)

    fun colorTertiaryDark(): Preference<Int> = preferenceStore.getInt("color_tertiary_dark", 0)

    fun startScreen(): Preference<StartScreen> =
        preferenceStore.getJsonObject(
            "start_screen",
            StartScreen.Library,
            StartScreen.serializer(),
        )

    fun confirmExit(): Preference<Boolean> = preferenceStore.getBoolean("confirm_exit", false)

    fun language(): Preference<String> = preferenceStore.getString("language", "")

    fun dateFormat(): Preference<String> = preferenceStore.getString("date_format", "")

    fun incognito(): Preference<Boolean> = preferenceStore.getBoolean("incognito", false)
}
