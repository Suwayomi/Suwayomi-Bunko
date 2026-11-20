/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.util.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

val Long.color get() = Color(this)

fun Color.toHexString(): String = String.format("#%06X", (0xFFFFFF and toArgb()))

fun Color.toLong() = String.format("%06X", 0xFFFFFF and toArgb()).toLong(16)
