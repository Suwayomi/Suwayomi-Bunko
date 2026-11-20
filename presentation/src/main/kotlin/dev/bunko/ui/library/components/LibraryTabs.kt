/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import dev.bunko.domain.category.model.Category
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTabs(
    pagerState: PagerState,
    categories: ImmutableList<Category>,
    onPageChanged: (Int) -> Unit,
) {
    if (categories.size <= 1) return

    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = currentPageIndex,
            edgePadding = 0.dp,
            divider = {},
        ) {
            categories.fastForEachIndexed { index, category ->
                Tab(
                    selected = currentPageIndex == index,
                    onClick = { onPageChanged(index) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.name)
                            if (category.size > 0) {
                                Spacer(Modifier.width(4.dp))
                                CountPill(category.size.toString())
                            }
                        }
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun CountPill(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
) {
    val alpha = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.12f else 0.08f
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
        contentColor = LocalContentColor.current,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}
