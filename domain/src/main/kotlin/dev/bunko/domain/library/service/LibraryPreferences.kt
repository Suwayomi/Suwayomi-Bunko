/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.library.service

import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.PreferenceStore
import dev.bunko.domain.chapter.model.ChapterListOptions
import dev.bunko.domain.library.model.FilterState
import dev.bunko.domain.library.model.Sort

class LibraryPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun showAllCategory(): Preference<Boolean> = preferenceStore.getBoolean("show_all_category", false)

    fun filterDownloaded(): Preference<FilterState> =
        preferenceStore.getJsonObject(
            "filter_downloaded",
            FilterState.IGNORED,
            FilterState.serializer(),
        )

    fun filterUnread(): Preference<FilterState> =
        preferenceStore.getJsonObject(
            "filter_unread",
            FilterState.IGNORED,
            FilterState.serializer(),
        )

    fun filterCompleted(): Preference<FilterState> =
        preferenceStore.getJsonObject(
            "filter_completed",
            FilterState.IGNORED,
            FilterState.serializer(),
        )

    fun sortMode(): Preference<Sort> = preferenceStore.getJsonObject("sort_mode", Sort.ALPHABETICAL, Sort.serializer())

    fun sortAscending(): Preference<Boolean> = preferenceStore.getBoolean("sort_ascending", true)

    fun displayMode(): Preference<dev.bunko.domain.library.model.DisplayMode> =
        preferenceStore.getJsonObject(
            "display_mode",
            dev.bunko.domain.library.model.DisplayMode.CompactGrid,
            dev.bunko.domain.library.model.DisplayMode.serializer(),
        )

    fun gridColumns(): Preference<Int> = preferenceStore.getInt("grid_columns", 0)

    fun gridColumnsLandscape(): Preference<Int> = preferenceStore.getInt("grid_columns_landscape", 0)

    fun chapterListDefaults(): Preference<ChapterListOptions> =
        preferenceStore.getJsonObject(
            "chapter_list_defaults",
            ChapterListOptions(),
            ChapterListOptions.serializer(),
        )

    fun gridSize(): Preference<Int> = preferenceStore.getInt("grid_size", 160)

    fun unreadBadge(): Preference<Boolean> = preferenceStore.getBoolean("unread_badge", true)

    fun downloadBadge(): Preference<Boolean> = preferenceStore.getBoolean("download_badge", false)

    fun languageBadge(): Preference<Boolean> = preferenceStore.getBoolean("language_badge", false)

    fun localBadge(): Preference<Boolean> = preferenceStore.getBoolean("local_badge", false)
}
