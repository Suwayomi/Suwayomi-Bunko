/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.prefs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

interface PreferenceStore {
    fun getString(
        key: String,
        defaultValue: String = "",
    ): Preference<String>

    fun getLong(
        key: String,
        defaultValue: Long = 0,
    ): Preference<Long>

    fun getInt(
        key: String,
        defaultValue: Int = 0,
    ): Preference<Int>

    fun getFloat(
        key: String,
        defaultValue: Float = 0f,
    ): Preference<Float>

    fun getBoolean(
        key: String,
        defaultValue: Boolean = false,
    ): Preference<Boolean>

    fun getStringSet(
        key: String,
        defaultValue: Set<String> = emptySet(),
    ): Preference<Set<String>>

    fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T>

    fun <T> getJsonObject(
        key: String,
        defaultValue: T,
        serializer: KSerializer<T>,
        serializersModule: SerializersModule = EmptySerializersModule(),
    ): Preference<T>
}
