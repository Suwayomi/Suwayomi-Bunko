/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

internal fun <T> Flow<T>.spaced(gap: Duration): Flow<T> {
    val source = conflate()
    return flow {
        source.collect {
            emit(it)
            delay(gap)
        }
    }
}
