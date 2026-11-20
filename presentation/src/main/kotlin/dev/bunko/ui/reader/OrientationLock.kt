/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import androidx.compose.runtime.staticCompositionLocalOf

enum class OrientationLock { Unlocked, Portrait, Landscape }

class OrientationController(
    val current: OrientationLock,
    val onChange: (OrientationLock) -> Unit,
)

val LocalOrientationController = staticCompositionLocalOf<OrientationController?> { null }
