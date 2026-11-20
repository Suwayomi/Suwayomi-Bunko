/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.prefs

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import me.tatarka.inject.annotations.Inject

@Inject
class PreferenceStoreFactory(
    private val context: Context,
) {
    fun create(vararg names: String): PreferenceStore =
        StandardPreferenceStore(
            SharedPreferencesSettings(
                context.getSharedPreferences(
                    names.joinToString(separator = "_"),
                    Context.MODE_PRIVATE,
                ),
            ),
        )
}
