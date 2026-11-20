/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.bunko.android.data.notification.Notifications
import dev.bunko.android.data.download.AndroidDownloadService
import dev.bunko.android.data.library.AndroidLibraryService
import dev.bunko.android.data.offline.OfflineDownloadService
import dev.bunko.domain.download.service.DownloadService
import dev.bunko.domain.library.service.LibraryUpdateService
import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.getAsFlow
import dev.bunko.domain.discord.PresenceLabels
import dev.bunko.domain.ui.model.ThemeMode
import dev.bunko.i18n.MR
import dev.icerock.moko.resources.desc.desc
import dev.icerock.moko.resources.format
import kotlinx.coroutines.flow.MutableStateFlow
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale

class App :
    Application(),
    DefaultLifecycleObserver {
    override fun onCreate() {
        super<Application>.onCreate()

        if (BuildConfig.DEBUG) {
            System.setProperty("kotlinx.coroutines.debug", "on")
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val appComponent = AppComponent.getInstance(this)

        setupNotificationChannels()

        appComponent.uiPreferences.themeMode()
            .getAsFlow {
                AppCompatDelegate.setDefaultNightMode(
                    when (it) {
                        ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
                        ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                    },
                )
            }
            .launchIn(ProcessLifecycleOwner.get().lifecycleScope)

        setupAppLanguage(appComponent.uiPreferences.language())

        appComponent.discordPresence.start(
            scope = ProcessLifecycleOwner.get().lifecycleScope,
            foreground = foreground,
            labels = PresenceLabels(
                appName = MR.strings.app_name.desc().toString(this),
                browsing = MR.strings.discord_presence_browsing.desc().toString(this),
                chapter = { MR.strings.discord_presence_chapter.format(it).toString(this) },
            ),
        )

        val processScope = ProcessLifecycleOwner.get().lifecycleScope
        appComponent.offlineChapters.active
            .filter { it }
            .onEach { OfflineDownloadService.start(this) }
            .launchIn(processScope)
        DownloadService.isDownloading
            .filter { it }
            .onEach { AndroidDownloadService.start(this) }
            .launchIn(processScope)
        LibraryUpdateService.isUpdating
            .filter { it }
            .onEach { AndroidLibraryService.start(this) }
            .launchIn(processScope)
        appComponent.serverSubscriptions.start(processScope)
    }

    private val foreground = MutableStateFlow(false)

    override fun onStart(owner: LifecycleOwner) {
        foreground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground.value = false
    }

    private fun setupNotificationChannels() {
        try {
            Notifications.createChannels(this)
        } catch (e: Exception) {
            log.error(e) { "Failed to modify notification channels" }
        }
    }

    private fun setupAppLanguage(languagePref: Preference<String>) {
        languagePref
            .getAsFlow {
                if (languagePref.isSet() && languagePref.defaultValue() != it) {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.create(
                            Locale.forLanguageTag(it),
                            Locale.forLanguageTag("en"),
                        ),
                    )
                } else if (
                    AppCompatDelegate.getApplicationLocales().isEmpty.not() &&
                    languagePref.isSet() &&
                    it == languagePref.defaultValue()
                ) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
            }
            .launchIn(ProcessLifecycleOwner.get().lifecycleScope)
    }

    private companion object {
        private val log = logging()
    }
}
