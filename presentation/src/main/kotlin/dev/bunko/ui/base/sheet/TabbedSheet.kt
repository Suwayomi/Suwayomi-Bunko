/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.sheet

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.bunko.uicore.resources.stringResource
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
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { page ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                content(page)
            }
        }
    }
}
