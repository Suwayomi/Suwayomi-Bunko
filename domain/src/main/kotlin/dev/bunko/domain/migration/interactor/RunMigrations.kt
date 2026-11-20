/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.migration.interactor

import dev.bunko.domain.BuildConfig
import dev.bunko.domain.migration.service.MigrationPreferences
import dev.bunko.domain.reader.service.ReaderPreferences
import me.tatarka.inject.annotations.Inject

@Inject
class RunMigrations(
    private val migrationPreferences: MigrationPreferences,
    private val readerPreferences: ReaderPreferences,
) {
    fun runMigrations() {
        val oldVersion = migrationPreferences.version().get()
        if (oldVersion < BuildConfig.MIGRATION_CODE) {
            migrationPreferences.version().set(BuildConfig.MIGRATION_CODE)

            if (oldVersion == 0) {
                readerPreferences.modes().get().forEach {
                    readerPreferences.getMode(it).direction().delete()
                }
                return
            }
        }
    }
}
