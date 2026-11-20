/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader.viewer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

@Composable
fun Modifier.readerZoom(
    pagerAxis: Orientation?,
    active: Boolean = true,
    onLongPress: () -> Unit = {},
    onTap: (position: Offset, size: IntSize) -> Unit,
): Modifier {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    var pendingTapJob by remember { mutableStateOf<Job?>(null) }
    var pendingTapPosition by remember { mutableStateOf<Offset?>(null) }
    var scale by remember { mutableFloatStateOf(1F) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(active) {
        if (!active) {
            zoomJob?.cancel()
            scale = 1F
            offset = Offset.Zero
        }
    }

    fun atEdge(drag: Offset, axis: Orientation): Boolean {
        val (along, across) = if (axis == Orientation.Horizontal) drag.x to drag.y else drag.y to drag.x
        if (abs(along) < abs(across)) return false
        val max = (if (axis == Orientation.Horizontal) size.width else size.height) * (scale - 1F) / 2F
        val position = if (axis == Orientation.Horizontal) offset.x else offset.y
        return if (along < 0) position <= -max + EDGE_EPSILON else position >= max - EDGE_EPSILON
    }

    suspend fun animateDoubleTap(tap: Offset) {
        val startScale = scale
        val startOffset = offset
        val zoomIn = startScale <= 1.01F
        val targetScale = if (zoomIn) DOUBLE_TAP_ZOOM else 1F
        val focal = tap - Offset(size.width / 2F, size.height / 2F)
        animate(0F, 1F, animationSpec = tween(DOUBLE_TAP_DURATION_MS, easing = FastOutSlowInEasing)) { t, _ ->
            val s = lerp(startScale, targetScale, t)
            scale = s
            offset = if (zoomIn) {
                val maxX = size.width * (s - 1F) / 2F
                val maxY = size.height * (s - 1F) / 2F
                val o = focal - (focal - startOffset) * (s / startScale)
                Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
            } else {
                startOffset * (1F - t)
            }
        }
        if (!zoomIn) offset = Offset.Zero
    }

    fun panBy(pan: Offset, claimsY: Boolean) {
        val maxX = size.width * (scale - 1F) / 2F
        val maxY = size.height * (scale - 1F) / 2F
        offset = Offset(
            (offset.x + pan.x).coerceIn(-maxX, maxX),
            (if (claimsY) offset.y + pan.y else offset.y).coerceIn(-maxY, maxY),
        )
    }

    fun flushPendingTap() {
        val position = pendingTapPosition ?: return
        pendingTapJob?.cancel()
        pendingTapJob = null
        pendingTapPosition = null
        currentOnTap(position, size)
    }

    return this
        .onSizeChanged { size = it }
        .pointerInput(pagerAxis) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                zoomJob?.cancel()
                pendingTapPosition?.let {
                    if ((down.position - it).getDistance() >= DOUBLE_TAP_SLOP.toPx()) flushPendingTap()
                }
                var wasMultiTouch = false
                var moved = false
                var drag = DragIntent.Undecided
                var travelled = Offset.Zero
                var longPressed = false
                var lastEvent: PointerEvent = currentEvent
                do {
                    val event = if (longPressed || moved || wasMultiTouch) {
                        awaitPointerEvent(PointerEventPass.Initial)
                    } else {
                        val remaining = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis - lastEvent.changes.first().uptimeMillis
                        withTimeoutOrNull(remaining.coerceAtLeast(1L)) { awaitPointerEvent(PointerEventPass.Initial) }
                    }
                    if (event == null) {
                        longPressed = true
                        currentOnLongPress()
                        continue
                    }
                    lastEvent = event
                    val multiTouch = event.changes.count { it.pressed } > 1
                    wasMultiTouch = wasMultiTouch || multiTouch
                    if ((event.changes.first().position - down.position).getDistance() > viewConfiguration.touchSlop) {
                        moved = true
                    }
                    if (multiTouch) {
                        drag = DragIntent.Pan
                        scale = (scale * event.calculateZoom()).coerceIn(1F, MAX_ZOOM)
                        panBy(event.calculatePan(), claimsY = true)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } else if (scale > 1F && drag != DragIntent.Swipe) {
                        val pan = event.calculatePan()
                        if (pagerAxis == null) {
                            panBy(pan, claimsY = false)
                        } else {
                            if (drag == DragIntent.Undecided) {
                                travelled += pan
                                if (travelled.getDistance() > viewConfiguration.touchSlop) {
                                    drag = if (atEdge(travelled, pagerAxis)) DragIntent.Swipe else DragIntent.Pan
                                }
                            }
                            if (drag != DragIntent.Swipe) panBy(pan, claimsY = true)
                            if (drag == DragIntent.Pan) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    }
                } while (event == null || event.changes.any { it.pressed })

                val up = lastEvent.changes.first()
                val isTap = !wasMultiTouch && !moved && !longPressed &&
                    up.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis
                if (!isTap) {
                    lastTapTime = 0L
                    flushPendingTap()
                    return@awaitEachGesture
                }
                val isDoubleTap = lastTapTime != 0L &&
                    down.uptimeMillis - lastTapTime < viewConfiguration.doubleTapTimeoutMillis &&
                    (down.position - lastTapPosition).getDistance() < DOUBLE_TAP_SLOP.toPx()
                if (isDoubleTap) {
                    lastTapTime = 0L
                    pendingTapJob?.cancel()
                    pendingTapJob = null
                    pendingTapPosition = null
                    lastEvent.changes.forEach { it.consume() }
                    zoomJob = scope.launch { animateDoubleTap(down.position) }
                    return@awaitEachGesture
                }

                val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                if (finalEvent.changes.any { it.isConsumed }) {
                    lastTapTime = 0L
                    return@awaitEachGesture
                }
                flushPendingTap()
                lastTapTime = up.uptimeMillis
                lastTapPosition = down.position
                val tapPosition = down.position
                pendingTapPosition = tapPosition
                pendingTapJob = scope.launch {
                    delay(viewConfiguration.doubleTapTimeoutMillis)
                    pendingTapJob = null
                    pendingTapPosition = null
                    currentOnTap(tapPosition, size)
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
        }
}

private enum class DragIntent { Undecided, Pan, Swipe }

private const val MAX_ZOOM = 4F
private const val EDGE_EPSILON = 1F
private const val DOUBLE_TAP_ZOOM = 2.2F
private const val DOUBLE_TAP_DURATION_MS = 250
private val DOUBLE_TAP_SLOP = 64.dp
