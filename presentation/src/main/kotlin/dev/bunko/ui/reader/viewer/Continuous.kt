/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader.viewer

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.bunko.domain.reader.model.Direction
import dev.bunko.ui.base.model.StableHolder
import dev.bunko.ui.reader.ChapterSeparator
import dev.bunko.ui.reader.ReaderImage
import dev.bunko.ui.reader.model.MoveTo
import dev.bunko.ui.reader.model.PageMove
import dev.bunko.ui.reader.model.ReaderChapter
import dev.bunko.ui.reader.model.ReaderItem
import dev.bunko.ui.reader.model.ReaderPage
import dev.bunko.ui.reader.model.ReaderPageSeparator
import dev.bunko.uicore.components.HorizontalScrollbar
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest

@Composable
fun ContinuousReader(
    modifier: Modifier,
    pages: ImmutableList<ReaderItem>,
    direction: Direction,
    maxSize: Int,
    padding: Int,
    currentPage: ReaderItem?,
    currentPageOffset: Int,
    loadingModifier: Modifier,
    pageContentScale: ContentScale,
    pageEmitterHolder: StableHolder<SharedFlow<PageMove>>,
    retry: (ReaderPage) -> Unit,
    progress: (ReaderItem) -> Unit,
    updateLastPageReadOffset: (Int) -> Unit,
    requestPreloadChapter: (ReaderChapter) -> Unit,
    autoScroll: Boolean,
    autoScrollSpeed: Int,
) {
    BoxWithConstraints(modifier then Modifier.fillMaxSize()) {
        val state = rememberLazyListState(pages.indexOf(currentPage).coerceAtLeast(1), currentPageOffset)
        val density = LocalDensity.current
        LaunchedEffect(Unit) {
            pageEmitterHolder.item
                .mapLatest { pageMove ->
                    when (pageMove) {
                        is PageMove.Direction -> {
                            val (moveTo) = pageMove
                            val by = when (moveTo) {
                                MoveTo.Previous -> -maxHeight
                                MoveTo.Next -> maxHeight
                            } * 0.8F
                            state.animateScrollBy(
                                with(density) {
                                    by.toPx()
                                },
                            )
                            Unit
                        }

                        is PageMove.Page -> {
                            val pageNumber = pages.indexOf(pageMove.page)
                            if (pageNumber > -1) {
                                state.animateScrollToItem(pageNumber)
                            }
                        }
                    }
                }
                .launchIn(this)
        }
        if (autoScroll && direction.isVertical) {
            val pxPerSecond = with(density) { autoScrollSpeed.dp.toPx() }
            LaunchedEffect(pxPerSecond) { state.autoScroll(pxPerSecond) }
        }
        val currentPages by rememberUpdatedState(pages)
        val currentProgress by rememberUpdatedState(progress)
        val currentUpdateOffset by rememberUpdatedState(updateLastPageReadOffset)
        DisposableEffect(Unit) {
            onDispose {
                currentUpdateOffset(state.progressItemOffset())
            }
        }
        LaunchedEffect(state) {
            snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { index -> currentPages.getOrNull(index)?.let(currentProgress) }
        }

        val imageModifier = if (maxSize != 0) {
            when (direction.isVertical) {
                true -> Modifier.width(maxSize.dp)
                false -> Modifier.height(maxSize.dp)
            }
        } else {
            when (pageContentScale) {
                ContentScale.FillWidth -> Modifier.fillMaxWidth()
                ContentScale.FillHeight -> Modifier.fillMaxHeight()
                else -> Modifier
            }
        }
        val contentPadding = when (direction) {
            Direction.Right -> PaddingValues(end = padding.dp)
            Direction.Left -> PaddingValues(start = padding.dp)
            Direction.Up -> PaddingValues(top = padding.dp)
            Direction.Down -> PaddingValues(bottom = padding.dp)
        }

        fun retry(index: Int) {
            pages.find { it is ReaderPage && it.index2 == index }?.let { retry(it as ReaderPage) }
        }

        when (direction.isVertical) {
            true -> {
                LazyColumn(
                    state = state,
                    reverseLayout = direction == Direction.Up,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(
                        modifier = Modifier.fillMaxWidth()
                            .padding(contentPadding),
                        pages = pages,
                        imageModifier = imageModifier,
                        loadingModifier = loadingModifier,
                        pageContentScale = pageContentScale,
                        retry = ::retry,
                        requestPreloadChapter = requestPreloadChapter,
                    )
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(state),
                    reverseLayout = direction == Direction.Up,
                )
            }

            false -> {
                LazyRow(
                    state = state,
                    reverseLayout = direction == Direction.Left,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    items(
                        modifier = Modifier.fillMaxHeight()
                            .padding(contentPadding),
                        pages = pages,
                        imageModifier = imageModifier,
                        loadingModifier = loadingModifier,
                        pageContentScale = pageContentScale,
                        retry = ::retry,
                        requestPreloadChapter = requestPreloadChapter,
                    )
                }
                HorizontalScrollbar(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    adapter = rememberScrollbarAdapter(state),
                    reverseLayout = direction == Direction.Left,
                )
            }
        }
    }
}

private fun LazyListState.progressItemOffset(): Int {
    val item = layoutInfo.visibleItemsInfo.lastOrNull() ?: return 0
    return (layoutInfo.viewportStartOffset - item.offset).coerceAtLeast(0)
}

private suspend fun LazyListState.autoScroll(pxPerSecond: Float) {
    while (true) {
        try {
            scroll {
                var last = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    val consumed = scrollBy(pxPerSecond * (now - last) / 1_000_000_000F)
                    last = now
                    if (consumed == 0F && !canScrollForward) return@scroll
                }
            }
            snapshotFlow { canScrollForward }.first { it }
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
        }
        withFrameNanos { }
        snapshotFlow { isScrollInProgress }.first { !it }
    }
}

private fun LazyListScope.items(
    modifier: Modifier,
    pages: ImmutableList<ReaderItem>,
    imageModifier: Modifier,
    loadingModifier: Modifier,
    pageContentScale: ContentScale,
    retry: (Int) -> Unit,
    requestPreloadChapter: (ReaderChapter) -> Unit,
) {
    items(
        pages,
        key = {
            when (it) {
                is ReaderPage -> it.chapter.chapter.index to it.index2
                is ReaderPageSeparator -> it.previousChapter?.chapter?.index to it.nextChapter?.chapter?.index
            }
        },
    ) { image ->
        when (image) {
            is ReaderPage -> Box(modifier, contentAlignment = Alignment.Center) {
                ReaderImage(
                    imageIndex = image.index2,
                    drawableHolder = image.bitmap.collectAsState().value,
                    bitmapInfo = image.bitmapInfo.collectAsState().value,
                    progress = image.progress.collectAsState().value,
                    status = image.status.collectAsState().value,
                    error = image.error.collectAsState().value,
                    imageModifier = imageModifier,
                    loadingModifier = loadingModifier,
                    contentScale = pageContentScale,
                    retry = retry,
                )
            }

            is ReaderPageSeparator -> ChapterSeparator(
                previousChapter = image.previousChapter,
                nextChapter = image.nextChapter,
                requestPreloadChapter = requestPreloadChapter,
            )
        }
    }
}
