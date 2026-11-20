/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.prefs

import com.russhwolf.settings.ObservableSettings
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule

class StandardPreferenceStore(
    private val preferences: ObservableSettings,
) : PreferenceStore {
    override fun getString(
        key: String,
        defaultValue: String,
    ): Preference<String> = StandardPreference(preferences, key, defaultValue, StringAdapter)

    override fun getLong(
        key: String,
        defaultValue: Long,
    ): Preference<Long> = StandardPreference(preferences, key, defaultValue, LongAdapter)

    override fun getInt(
        key: String,
        defaultValue: Int,
    ): Preference<Int> = StandardPreference(preferences, key, defaultValue, IntAdapter)

    override fun getFloat(
        key: String,
        defaultValue: Float,
    ): Preference<Float> = StandardPreference(preferences, key, defaultValue, FloatAdapter)

    override fun getBoolean(
        key: String,
        defaultValue: Boolean,
    ): Preference<Boolean> = StandardPreference(preferences, key, defaultValue, BooleanAdapter)

    override fun getStringSet(
        key: String,
        defaultValue: Set<String>,
    ): Preference<Set<String>> = StandardPreference(preferences, key, defaultValue, StringSetAdapter)

    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        val adapter = ObjectAdapter(serializer, deserializer)
        return StandardPreference(preferences, key, defaultValue, adapter)
    }

    override fun <T> getJsonObject(
        key: String,
        defaultValue: T,
        serializer: KSerializer<T>,
        serializersModule: SerializersModule,
    ): Preference<T> {
        val adapter = JsonObjectAdapter(defaultValue, serializer, serializersModule)
        return StandardPreference(preferences, key, defaultValue, adapter)
    }
}
