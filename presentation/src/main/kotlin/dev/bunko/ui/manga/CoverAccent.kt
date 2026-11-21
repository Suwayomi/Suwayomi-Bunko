/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.MaterialTheme as M2Theme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.diamondedge.logging.logging
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.manga.accent.CoverAccents
import dev.bunko.uicore.theme.atTone
import dev.bunko.uicore.theme.tone

private val log = logging()

private const val SAMPLE_SIZE = 32
private const val HUE_BINS = 24
private const val MIN_CONTRAST = 4.5

private val AccentSpec = tween<Color>(durationMillis = 350)

@Composable
fun CoverAccentTheme(
    mangaId: Long,
    manga: Manga?,
    accents: CoverAccents,
    content: @Composable (onCoverBitmap: (Bitmap) -> Unit) -> Unit,
) {
    val base = MaterialTheme.colorScheme
    val stored by accents.accents.collectAsState()
    val accent = stored[mangaId]?.argb?.let(::Color)
    val target = remember(base, accent) {
        accent?.let { base.withAccent(contrastSafeAccent(it, base.background), base.background) } ?: base
    }
    val scheme = target.animated()
    val colors = M2Theme.colors.copy(primary = scheme.primary, onPrimary = scheme.onPrimary)
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        M2Theme(colors = colors, typography = M2Theme.typography, shapes = M2Theme.shapes) {
            content { bitmap ->
                if (manga != null && !accents.isCurrent(manga)) accents.record(manga, bitmap)
            }
        }
    }
}

@Composable
private fun ColorScheme.animated(): ColorScheme =
    copy(
        primary = animateColorAsState(primary, AccentSpec, label = "primary").value,
        onPrimary = animateColorAsState(onPrimary, AccentSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(primaryContainer, AccentSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(onPrimaryContainer, AccentSpec, label = "onPrimaryContainer").value,
        secondaryContainer = animateColorAsState(secondaryContainer, AccentSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(onSecondaryContainer, AccentSpec, label = "onSecondaryContainer").value,
    )

internal fun dominantColor(bitmap: Bitmap): Color? {
    val small = runCatching {
        Bitmap.createScaledBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false), SAMPLE_SIZE, SAMPLE_SIZE, true)
    }.getOrElse {
        log.warn(it) { "Could not sample the cover" }
        return null
    }
    val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
    small.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
    val weight = FloatArray(HUE_BINS)
    val rgb = Array(HUE_BINS) { FloatArray(3) }
    val hsv = FloatArray(3)
    for (pixel in pixels) {
        if (android.graphics.Color.alpha(pixel) < 200) continue
        android.graphics.Color.colorToHSV(pixel, hsv)
        if (hsv[1] < 0.3f || hsv[2] < 0.25f) continue
        val w = hsv[1] * hsv[2]
        val bin = (hsv[0] / 360f * HUE_BINS).toInt() % HUE_BINS
        weight[bin] += w
        rgb[bin][0] += android.graphics.Color.red(pixel) * w
        rgb[bin][1] += android.graphics.Color.green(pixel) * w
        rgb[bin][2] += android.graphics.Color.blue(pixel) * w
    }
    val best = weight.indices.maxBy { weight[it] }
    if (weight[best] < 1f) return null
    val (r, g, b) = rgb[best].map { it / weight[best] / 255f }
    return Color(r, g, b)
}

internal fun contrastSafeAccent(
    accent: Color,
    background: Color,
): Color {
    val dark = background.luminance() < 0.5f
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(accent.toArgb(), it) }
    repeat(25) {
        val argb = ColorUtils.HSLToColor(hsl)
        if (ColorUtils.calculateContrast(argb, background.toArgb()) >= MIN_CONTRAST) return Color(argb)
        hsl[2] = (hsl[2] + if (dark) 0.04f else -0.04f).coerceIn(0f, 1f)
    }
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun ColorScheme.withAccent(
    accent: Color,
    background: Color,
): ColorScheme {
    val dark = background.tone() < 50
    val container = accent.atTone(if (dark) 30.0 else 90.0)
    val onContainer = accent.atTone(if (dark) 90.0 else 10.0)
    return copy(
        primary = accent,
        onPrimary = readableOn(accent),
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
        surfaceTint = accent,
    )
}

private fun readableOn(color: Color): Color =
    if (ColorUtils.calculateContrast(Color.White.toArgb(), color.toArgb()) >=
        ColorUtils.calculateContrast(Color.Black.toArgb(), color.toArgb())
    ) {
        Color.White
    } else {
        Color.Black
    }
