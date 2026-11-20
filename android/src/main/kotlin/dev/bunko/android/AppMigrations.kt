/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android

import dev.bunko.android.data.update.UpdateCheckWorker
import dev.bunko.domain.migration.service.MigrationPreferences
import dev.bunko.uicore.vm.ContextWrapper
import me.tatarka.inject.annotations.Inject

@Inject
class AppMigrations(
    private val migrationPreferences: MigrationPreferences,
    private val contextWrapper: ContextWrapper,
) {
    fun runMigrations(): Boolean {
        val oldVersion = migrationPreferences.appVersion().get()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            migrationPreferences.appVersion().set(BuildConfig.VERSION_CODE)

            UpdateCheckWorker.setupTask(contextWrapper)

            if (oldVersion == 0) {
                return false
            }
            return true
        }
        return false
    }
}
