/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.prefs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

interface Preference<T> {
    fun key(): String

    fun get(): T

    fun set(value: T)

    fun isSet(): Boolean

    fun delete()

    fun defaultValue(): T

    fun changes(): Flow<T>

    fun stateIn(scope: CoroutineScope): StateFlow<T>
}

fun <T> Preference<T>.getAsFlow(action: (suspend (T) -> Unit)? = null): Flow<T> {
    val flow = changes()
        .onStart { emit(get()) }
    return if (action != null) {
        flow.onEach(action = action)
    } else {
        flow
    }
}
