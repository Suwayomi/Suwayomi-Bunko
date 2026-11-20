/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.uicore.components

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

interface ScrollbarAdapter

class ScrollStateScrollbarAdapter(
    val scrollState: ScrollState,
) : ScrollbarAdapter

class LazyListStateScrollbarAdapter(
    val lazyListState: LazyListState,
) : ScrollbarAdapter

class LazyGridStateScrollbarAdapter(
    val lazyGridState: LazyGridState,
    val gridCells: GridCells,
    val spacing: Dp,
) : ScrollbarAdapter

@Composable
fun VerticalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
) {
    val scrollbarModifier = when (adapter) {
        is ScrollStateScrollbarAdapter -> {
            Modifier.drawScrollbar(adapter.scrollState, Orientation.Vertical, reverseLayout)
        }

        is LazyListStateScrollbarAdapter -> {
            Modifier.drawScrollbar(adapter.lazyListState, Orientation.Vertical, reverseLayout)
        }

        is LazyGridStateScrollbarAdapter -> {
            Modifier.drawScrollbar(adapter.lazyGridState, adapter.gridCells, adapter.spacing, Orientation.Vertical, reverseLayout)
        }

        else -> Modifier
    }
    Box(modifier then Modifier.fillMaxSize() then scrollbarModifier)
}

@Composable
fun HorizontalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
) {
    val scrollbarModifier = when (adapter) {
        is ScrollStateScrollbarAdapter -> {
            Modifier.drawScrollbar(adapter.scrollState, Orientation.Horizontal, reverseLayout)
        }

        is LazyListStateScrollbarAdapter -> {
            Modifier.drawScrollbar(adapter.lazyListState, Orientation.Horizontal, reverseLayout)
        }

        else -> Modifier
    }
    Box(modifier then Modifier.fillMaxSize() then scrollbarModifier)
}

@Composable
fun rememberScrollbarAdapter(scrollState: ScrollState): ScrollbarAdapter =
    remember(scrollState) {
        ScrollStateScrollbarAdapter(scrollState)
    }

@Composable
fun rememberScrollbarAdapter(scrollState: LazyListState): ScrollbarAdapter =
    remember(scrollState) {
        LazyListStateScrollbarAdapter(scrollState)
    }

@Composable
fun rememberVerticalScrollbarAdapter(
    scrollState: LazyGridState,
    gridCells: GridCells,
    arrangement: Arrangement.Vertical? = null,
): ScrollbarAdapter =
    remember(scrollState, gridCells) {
        LazyGridStateScrollbarAdapter(scrollState, gridCells, arrangement?.spacing ?: Dp.Hairline)
    }


private fun Modifier.drawScrollbar(
    state: ScrollState,
    orientation: Orientation,
    reverseScrolling: Boolean,
): Modifier =
    drawScrollbar(
        orientation = orientation,
        reverseScrolling = reverseScrolling,
        scrollFlow = snapshotFlow { state.isScrollInProgress },
    ) { reverseDirection, atEnd, thickness, color, alpha ->
        val showScrollbar = state.maxValue > 0
        val canvasSize = if (orientation == Orientation.Horizontal) size.width else size.height
        val totalSize = canvasSize + state.maxValue
        val thumbSize = canvasSize / totalSize * canvasSize
        val startOffset = state.value / totalSize * canvasSize
        val drawScrollbar = onDrawScrollbar(
            orientation = orientation,
            reverseDirection = reverseDirection,
            atEnd = atEnd,
            showScrollbar = showScrollbar,
            thickness = thickness,
            color = color,
            alpha = alpha,
            thumbSize = thumbSize,
            startOffset = startOffset,
        )
        onDrawWithContent {
            drawContent()
            drawScrollbar()
        }
    }

private fun Modifier.drawScrollbar(
    state: LazyListState,
    orientation: Orientation,
    reverseScrolling: Boolean,
): Modifier =
    drawScrollbar(
        orientation,
        reverseScrolling,
        snapshotFlow { state.isScrollInProgress },
    ) { reverseDirection, atEnd, thickness, color, alpha ->
        val layoutInfo = state.layoutInfo
        val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val items = layoutInfo.visibleItemsInfo
        val itemsSize = items.fastSumBy { it.size }
        val showScrollbar = items.size < layoutInfo.totalItemsCount || itemsSize > viewportSize
        val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
        val totalSize = estimatedItemSize * layoutInfo.totalItemsCount
        val canvasSize = if (orientation == Orientation.Horizontal) size.width else size.height
        val thumbSize = viewportSize / totalSize * canvasSize
        val startOffset = if (items.isEmpty()) {
            0f
        } else {
            items
                .first()
                .run {
                    (estimatedItemSize * index - offset) / totalSize * canvasSize
                }
        }
        val drawScrollbar = onDrawScrollbar(
            orientation = orientation,
            reverseDirection = reverseDirection,
            atEnd = atEnd,
            showScrollbar = showScrollbar,
            thickness = thickness,
            color = color,
            alpha = alpha,
            thumbSize = thumbSize,
            startOffset = startOffset,
        )
        onDrawWithContent {
            drawContent()
            drawScrollbar()
        }
    }

private fun Modifier.drawScrollbar(
    state: LazyGridState,
    gridCells: GridCells,
    spacing: Dp,
    orientation: Orientation,
    reverseScrolling: Boolean,
): Modifier =
    drawScrollbar(
        orientation,
        reverseScrolling,
        snapshotFlow { state.isScrollInProgress },
    ) { reverseDirection, atEnd, thickness, color, alpha ->
        val layoutInfo = state.layoutInfo
        val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val items = layoutInfo.visibleItemsInfo
        val itemsSize = items.chunked(
            with(gridCells) {
                calculateCrossAxisCellSizes(viewportSize, spacing.roundToPx()).size
            },
        ).sumOf { it.fastMaxBy { it.size.height }?.size?.height ?: 0 }
        val showScrollbar = items.size < layoutInfo.totalItemsCount || itemsSize > viewportSize
        val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
        val totalSize = estimatedItemSize * layoutInfo.totalItemsCount
        val canvasSize = if (orientation == Orientation.Horizontal) size.width else size.height
        val thumbSize = viewportSize / totalSize * canvasSize
        val startOffset = if (items.isEmpty()) {
            0f
        } else {
            items
                .first()
                .run {
                    (estimatedItemSize * index - if (orientation == Orientation.Vertical) offset.y else offset.x) / totalSize * canvasSize
                }
        }
        val drawScrollbar = onDrawScrollbar(
            orientation = orientation,
            reverseDirection = reverseDirection,
            atEnd = atEnd,
            showScrollbar = showScrollbar,
            thickness = thickness,
            color = color,
            alpha = alpha,
            thumbSize = thumbSize,
            startOffset = startOffset,
        )
        onDrawWithContent {
            drawContent()
            drawScrollbar()
        }
    }

private fun CacheDrawScope.onDrawScrollbar(
    orientation: Orientation,
    reverseDirection: Boolean,
    atEnd: Boolean,
    showScrollbar: Boolean,
    thickness: Float,
    color: Color,
    alpha: Float,
    thumbSize: Float,
    startOffset: Float,
): DrawScope.() -> Unit {
    val topLeft = if (orientation == Orientation.Horizontal) {
        Offset(
            if (reverseDirection) size.width - startOffset - thumbSize else startOffset,
            if (atEnd) size.height - thickness else 0f,
        )
    } else {
        Offset(
            if (atEnd) size.width - thickness else 0f,
            if (reverseDirection) size.height - startOffset - thumbSize else startOffset,
        )
    }
    val size = if (orientation == Orientation.Horizontal) {
        Size(thumbSize, thickness)
    } else {
        Size(thickness, thumbSize)
    }

    return {
        if (showScrollbar) {
            drawRect(
                color = color,
                topLeft = topLeft,
                size = size,
                alpha = alpha,
            )
        }
    }
}

private fun Modifier.drawScrollbar(
    orientation: Orientation,
    reverseScrolling: Boolean,
    scrollFlow: Flow<Boolean>,
    onBuildDrawCache: CacheDrawScope.(
        reverseDirection: Boolean,
        atEnd: Boolean,
        thickness: Float,
        color: Color,
        alpha: Float,
    ) -> DrawResult,
): Modifier =
    composed {
        val isScrollInProgress by scrollFlow.collectAsState(initial = false)
        val alpha = remember { Animatable(0f) }
        LaunchedEffect(isScrollInProgress, alpha) {
            if (isScrollInProgress) {
                alpha.snapTo(1f)
            } else {
                delay(ViewConfiguration.getScrollDefaultDelay().toLong())
                alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
            }
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val reverseDirection = if (orientation == Orientation.Horizontal) {
            if (isLtr) reverseScrolling else !reverseScrolling
        } else {
            reverseScrolling
        }
        val atEnd = if (orientation == Orientation.Vertical) isLtr else true

        val thickness = with(LocalDensity.current) { Thickness.toPx() }
        val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        Modifier
            .drawWithCache {
                onBuildDrawCache(reverseDirection, atEnd, thickness, color, alpha.value)
            }
    }

private val Thickness = 4.dp
private val FadeOutAnimationSpec =
    tween<Float>(
        durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
    )
