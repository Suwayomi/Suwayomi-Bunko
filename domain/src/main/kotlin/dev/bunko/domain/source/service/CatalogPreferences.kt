/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.source.service

import androidx.compose.ui.text.intl.Locale
import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.PreferenceStore
import dev.bunko.domain.library.model.DisplayMode

class CatalogPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun languages(): Preference<Set<String>> = preferenceStore.getStringSet("enabled_langs", setOfNotNull("en", Locale.current.language))

    fun pinnedSources(): Preference<Set<String>> = preferenceStore.getStringSet("pinned_sources", emptySet())

    fun globalSearchPinnedOnly(): Preference<Boolean> = preferenceStore.getBoolean("global_search_pinned_only", false)

    fun migrateChapters(): Preference<Boolean> = preferenceStore.getBoolean("migrate_chapters", true)

    fun migrateCategories(): Preference<Boolean> = preferenceStore.getBoolean("migrate_categories", true)

    fun migrateTracking(): Preference<Boolean> = preferenceStore.getBoolean("migrate_tracking", true)

    fun migrateDeleteDownloads(): Preference<Boolean> = preferenceStore.getBoolean("migrate_delete_downloads", false)

    fun displayMode(): Preference<DisplayMode> =
        preferenceStore.getJsonObject(
            "display_mode",
            DisplayMode.CompactGrid,
            DisplayMode.serializer(),
        )
}
