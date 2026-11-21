/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.uicore.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun Color.tone(): Double {
    val lab = DoubleArray(3)
    ColorUtils.colorToLAB(toArgb(), lab)
    return lab[0]
}

fun Color.chroma(): Double {
    val lab = DoubleArray(3)
    ColorUtils.colorToLAB(toArgb(), lab)
    return hypot(lab[1], lab[2])
}

fun Color.atTone(
    tone: Double,
    maxChroma: Double = Double.MAX_VALUE,
): Color {
    val lab = DoubleArray(3)
    ColorUtils.colorToLAB(toArgb(), lab)
    val hue = atan2(lab[2], lab[1])
    var chroma = hypot(lab[1], lab[2]).coerceAtMost(maxChroma)
    val target = tone.coerceIn(0.0, 100.0)
    val back = DoubleArray(3)
    repeat(16) {
        val argb = ColorUtils.LABToColor(target, chroma * cos(hue), chroma * sin(hue))
        ColorUtils.colorToLAB(argb, back)
        if (abs(back[0] - target) < 1.0 && abs(hypot(back[1], back[2]) - chroma) < 2.5) return Color(argb)
        chroma *= 0.85
    }
    return Color(ColorUtils.LABToColor(target, 0.0, 0.0))
}

fun Color.onColor(): Color = if (tone() >= 60) atTone(12.0, 24.0) else atTone(98.0, 6.0)
