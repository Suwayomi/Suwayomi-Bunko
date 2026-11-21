/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.uicore.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SkeletonLineShape = RoundedCornerShape(4.dp)

@Composable
fun SkeletonHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val colors = remember(highlight) { listOf(Color.Transparent, highlight, Color.Transparent) }
    val fade = remember { listOf(Color.Black, Color.Black, Color.Transparent) }
    val progress = rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100, delayMillis = 250, easing = LinearEasing)),
        label = "skeleton-sweep",
    )
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(220, delayMillis = 150)) }

    Box(
        modifier
            .clipToBounds()
            .graphicsLayer {
                alpha = reveal.value
                compositingStrategy = CompositingStrategy.Offscreen
            }.drawWithContent {
                drawContent()
                val band = size.width * 0.5f
                val x = -band + (size.width + band * 2) * progress.value
                drawRect(
                    brush = Brush.linearGradient(colors, Offset(x - band, 0f), Offset(x + band, band * 0.4f)),
                    blendMode = BlendMode.SrcAtop,
                )
                drawRect(brush = Brush.verticalGradient(fade), blendMode = BlendMode.DstIn)
            },
        content = content,
    )
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = SkeletonLineShape,
) {
    Spacer(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, shape))
}

@Composable
fun rememberSkeletonPulse(): State<Color> {
    val from = MaterialTheme.colorScheme.surfaceContainerHighest
    val to = MaterialTheme.colorScheme.surfaceContainerHigh
    return rememberInfiniteTransition(label = "skeleton-pulse").animateColor(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeleton-pulse-color",
    )
}

@Composable
fun rememberStaticSkeletonColor(): State<Color> {
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    return remember(color) { mutableStateOf(color) }
}

@Composable
fun SkeletonPulseBlock(
    pulse: State<Color>,
    modifier: Modifier = Modifier,
    shape: Shape = SkeletonLineShape,
) {
    Spacer(
        modifier.drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawBehind { drawOutline(outline, pulse.value) }
        },
    )
}

@Composable
fun SkeletonTextLine(
    modifier: Modifier = Modifier,
    fraction: Float = 1f,
    height: Dp = 12.dp,
) {
    SkeletonBlock(modifier.fillMaxWidth(fraction).height(height))
}

@Composable
fun SkeletonListRow(
    coverHeight: Dp,
    lines: Int,
    modifier: Modifier = Modifier,
    coverShape: Shape = RoundedCornerShape(8.dp),
    coverAspectRatio: Float = mangaAspectRatio,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(Modifier.height(coverHeight).aspectRatio(coverAspectRatio), coverShape)
        Column(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(lines) { line ->
                SkeletonTextLine(fraction = SkeletonLineWidths[line % SkeletonLineWidths.size])
            }
        }
    }
}

@Composable
fun SkeletonList(
    coverHeight: Dp,
    lines: Int,
    modifier: Modifier = Modifier,
    rows: Int = 12,
    coverAspectRatio: Float = mangaAspectRatio,
    coverShape: Shape = RoundedCornerShape(8.dp),
) {
    SkeletonHost(modifier.fillMaxSize()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            repeat(rows) { SkeletonListRow(coverHeight, lines, coverAspectRatio = coverAspectRatio, coverShape = coverShape) }
        }
    }
}

private val SkeletonLineWidths = floatArrayOf(0.7f, 0.45f, 0.3f)
