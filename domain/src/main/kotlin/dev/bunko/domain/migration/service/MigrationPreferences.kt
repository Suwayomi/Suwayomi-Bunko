/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.migration.service

import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.PreferenceStore

class MigrationPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun version(): Preference<Int> = preferenceStore.getInt("version", 0)

    fun appVersion(): Preference<Int> = preferenceStore.getInt("app_version", 0)
}
