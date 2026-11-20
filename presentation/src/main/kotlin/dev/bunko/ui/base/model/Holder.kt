/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.model

import androidx.compose.runtime.Stable

@Stable
class StableHolder<T>(
    val item: T,
) {
    operator fun component1(): T = item
}
