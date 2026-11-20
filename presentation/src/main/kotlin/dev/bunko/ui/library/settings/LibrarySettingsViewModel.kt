/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.settings

import dev.bunko.domain.library.service.LibraryPreferences
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import me.tatarka.inject.annotations.Inject

@Inject
class LibrarySettingsViewModel(
    libraryPreferences: LibraryPreferences,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val filterDownloaded = libraryPreferences.filterDownloaded().asStateFlow()
    val filterUnread = libraryPreferences.filterUnread().asStateFlow()
    val filterCompleted = libraryPreferences.filterCompleted().asStateFlow()

    val sortMode = libraryPreferences.sortMode().asStateFlow()
    val sortAscending = libraryPreferences.sortAscending().asStateFlow()

    val displayMode = libraryPreferences.displayMode().asStateFlow()
    val unreadBadges = libraryPreferences.unreadBadge().asStateFlow()
    val downloadBadges = libraryPreferences.downloadBadge().asStateFlow()
    val languageBadges = libraryPreferences.languageBadge().asStateFlow()
    val localBadges = libraryPreferences.localBadge().asStateFlow()
}
