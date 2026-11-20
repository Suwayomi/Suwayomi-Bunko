package dev.bunko.domain.user.service

import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.PreferenceStore

class UserPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun uiRefreshToken(): Preference<String> = preferenceStore.getString("ui_refresh_token", "")

    fun uiAccessToken(): Preference<String> = preferenceStore.getString("ui_access_token", "")

    fun simpleSession(): Preference<String> = preferenceStore.getString("simple_session", "")

    fun simpleUsername(): Preference<String> = preferenceStore.getString("simple_username", "")

    fun simplePassword(): Preference<String> = preferenceStore.getString("simple_password", "")
}
