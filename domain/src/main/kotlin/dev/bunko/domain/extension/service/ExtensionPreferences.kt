/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.extension.service

import androidx.compose.ui.text.intl.Locale
import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.PreferenceStore

class ExtensionPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun languages(): Preference<Set<String>> =
        preferenceStore.getStringSet(
            "enabled_langs",
            setOfNotNull("all", "en", Locale.current.language),
        )
}
