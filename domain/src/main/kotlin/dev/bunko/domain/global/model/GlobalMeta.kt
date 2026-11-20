/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.global.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class GlobalMeta(
    val example: Int = 0,
    val historyClearedAt: Long = 0,
) {
    companion object {
        const val KEY_HISTORY_CLEARED_AT = "@@bunkoHistoryClearedAt"
    }
}
