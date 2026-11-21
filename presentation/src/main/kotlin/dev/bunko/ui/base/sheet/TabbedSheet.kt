/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.sheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.theme.BunkoMotion
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedSheet(
    tabs: ImmutableList<StringResource>,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onPageChange: ((Int) -> Unit)? = null,
    content: @Composable ColumnScope.(page: Int) -> Unit,
) {
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    if (onPageChange != null) {
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect(onPageChange)
        }
    }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                divider = {},
            ) {
                tabs.fastForEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(stringResource(tab), fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
            trailing?.invoke()
        }
        HorizontalDivider()
        val pageHeights = remember { mutableStateMapOf<Int, Int>() }
        val height = remember { Animatable(0, Int.VectorConverter) }
        val targetHeight = pageHeights[pagerState.targetPage]
        LaunchedEffect(targetHeight) {
            if (targetHeight == null) return@LaunchedEffect
            if (height.value == 0) {
                height.snapTo(targetHeight)
            } else {
                height.animateTo(targetHeight, tween(BunkoMotion.LONG_MS, easing = BunkoMotion.EmphasizedDecelerate))
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val h = height.value.takeIf { it > 0 }
                        ?.coerceIn(constraints.minHeight, constraints.maxHeight)
                        ?: placeable.height
                    layout(placeable.width, h) { placeable.place(0, 0) }
                },
            beyondViewportPageCount = tabs.size,
            verticalAlignment = Alignment.Top,
        ) { page ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { pageHeights[page] = it.height }
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                content(page)
            }
        }
    }
}
