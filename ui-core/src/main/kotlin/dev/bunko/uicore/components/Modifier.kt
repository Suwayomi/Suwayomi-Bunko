/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.uicore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.selectedBackground(isSelected: Boolean): Modifier =
    composed {
        if (isSelected) {
            val alpha = if (isSystemInDarkTheme()) 0.08f else 0.22f
            background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha))
        } else {
            this
        }
    }
