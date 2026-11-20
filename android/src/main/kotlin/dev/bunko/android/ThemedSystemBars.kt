/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

private const val LIGHT_ICON_SCRIM = 0x4D000000

@Composable
internal fun ComponentActivity.ThemedSystemBars() {
    val darkIcons = MaterialTheme.colors.isLight
    LaunchedEffect(darkIcons) {
        val style = if (darkIcons) {
            SystemBarStyle.light(Color.TRANSPARENT, LIGHT_ICON_SCRIM)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
