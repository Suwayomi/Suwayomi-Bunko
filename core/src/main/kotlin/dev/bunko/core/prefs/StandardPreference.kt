/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.prefs

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class StandardPreference<T>(
    private val preferences: ObservableSettings,
    private val key: String,
    private val defaultValue: T,
    private val adapter: Adapter<T>,
) : Preference<T> {
    override fun key(): String = key

    override fun get(): T =
        if (isSet()) {
            adapter.get(key, preferences)
        } else {
            defaultValue
        }

    @OptIn(DelicateCoroutinesApi::class)
    override fun set(value: T) {
        adapter.set(key, value, preferences)
        GlobalScope.launch {
            listener.emit(key)
        }
    }

    override fun isSet(): Boolean = adapter.isSet(preferences.keys, key)

    override fun delete() {
        preferences.remove(key)
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> =
        listener
            .filter { it == key }
            .map { get() }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = changes().stateIn(scope, SharingStarted.Eagerly, get())

    companion object {
        private val listener = MutableSharedFlow<String>()
    }
}
