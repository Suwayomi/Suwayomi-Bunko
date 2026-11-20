/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader.viewer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest

@Composable
fun PagerReader(
    modifier: Modifier,
    direction: Direction,
    currentPage: ReaderItem?,
    pages: ImmutableList<ReaderItem>,
    loadingModifier: Modifier,
    pageContentScale: ContentScale,
    pageEmitterHolder: StableHolder<SharedFlow<PageMove>>,
    onTap: (position: Offset, size: IntSize) -> Unit,
    onLongPress: () -> Unit,
    retry: (ReaderPage) -> Unit,
    progress: (ReaderItem) -> Unit,
    requestPreloadChapter: (ReaderChapter) -> Unit,
) {
    val pageCount by rememberUpdatedState(pages.size)
    val state = rememberPagerState(initialPage = pages.indexOf(currentPage).coerceAtLeast(1)) { pageCount }
    val currentPageState = rememberUpdatedState(currentPage)

    LaunchedEffect(pages.size, state, currentPageState) {
        val pageRange = pages.indices
        pageEmitterHolder.item
            .mapLatest { pageMove ->
                when (pageMove) {
                    is PageMove.Direction -> {
                        val currentPage = currentPageState.value ?: return@mapLatest
                        val page = when (pageMove.moveTo) {
                            MoveTo.Previous -> pages.indexOf(currentPage) - 1
                            MoveTo.Next -> pages.indexOf(currentPage) + 1
                        }
                        if (page in pageRange) state.animateScrollToPage(page)
                    }

                    is PageMove.Page -> {
                        val pageNumber = pages.indexOf(pageMove.page)
                        if (pageNumber > -1) state.animateScrollToPage(pageNumber)
                    }
                }
            }
            .launchIn(this)
    }

    LaunchedEffect(state.currentPage) {
        pages.getOrNull(state.currentPage)?.let(progress)
    }

    fun retry(index: Int) {
        pages.find { it is ReaderPage && it.index2 == index }?.let { retry(it as ReaderPage) }
    }

    val axis = if (direction.isVertical) Orientation.Vertical else Orientation.Horizontal
    val fling = PagerDefaults.flingBehavior(state, pagerSnapDistance = PagerSnapDistance.atMost(1))
    val key = { index: Int -> pageKey(pages.getOrNull(index), index) }
    val page: @Composable (Int) -> Unit = { index ->
        PagerPage(
            item = pages[index],
            axis = axis,
            settled = index == state.settledPage,
            loadingModifier = loadingModifier,
            pageContentScale = pageContentScale,
            onTap = onTap,
            onLongPress = onLongPress,
            retry = ::retry,
            requestPreloadChapter = requestPreloadChapter,
        )
    }
    if (direction.isVertical) {
        VerticalPager(
            state = state,
            modifier = modifier.fillMaxSize(),
            reverseLayout = direction == Direction.Up,
            beyondViewportPageCount = 1,
            flingBehavior = fling,
            key = key,
        ) { page(it) }
    } else {
        HorizontalPager(
            state = state,
            modifier = modifier.fillMaxSize(),
            reverseLayout = direction == Direction.Left,
            beyondViewportPageCount = 1,
            flingBehavior = fling,
            key = key,
        ) { page(it) }
    }
}

private fun pageKey(item: ReaderItem?, index: Int): Any = when (item) {
    is ReaderPage -> item.chapter.chapter.index to item.index2
    is ReaderPageSeparator -> item.previousChapter?.chapter?.index to item.nextChapter?.chapter?.index
    else -> index
}

@Composable
private fun PagerPage(
    item: ReaderItem,
    axis: Orientation,
    settled: Boolean,
    loadingModifier: Modifier,
    pageContentScale: ContentScale,
    onTap: (position: Offset, size: IntSize) -> Unit,
    onLongPress: () -> Unit,
    retry: (Int) -> Unit,
    requestPreloadChapter: (ReaderChapter) -> Unit,
) {
    Box(
        Modifier.fillMaxSize().readerZoom(pagerAxis = axis, active = settled, onLongPress = onLongPress, onTap = onTap),
        contentAlignment = Alignment.Center,
    ) {
        when (item) {
            is ReaderPage -> ReaderImage(
                imageIndex = item.index2,
                drawableHolder = item.bitmap.collectAsState().value,
                bitmapInfo = item.bitmapInfo.collectAsState().value,
                progress = item.progress.collectAsState().value,
                status = item.status.collectAsState().value,
                error = item.error.collectAsState().value,
                loadingModifier = loadingModifier,
                retry = retry,
                contentScale = pageContentScale,
            )

            is ReaderPageSeparator -> ChapterSeparator(
                previousChapter = item.previousChapter,
                nextChapter = item.nextChapter,
                requestPreloadChapter = requestPreloadChapter,
            )
        }
    }
}
