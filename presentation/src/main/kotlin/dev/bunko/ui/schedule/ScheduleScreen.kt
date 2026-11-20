/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.ActionItem
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.manga.MangaScreen
import dev.bunko.ui.schedule.ScheduleViewModel.State
import dev.bunko.ui.schedule.components.ScheduleFilterBar
import dev.bunko.ui.schedule.components.ScheduleRow
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadingScreen
import dev.bunko.uicore.resources.stringResource
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

class ScheduleScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { scheduleViewModel() }
        val state by vm.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val actions = remember(vm, navigator) {
            ScheduleActions(
                refresh = vm::refresh,
                open = { navigator push MangaScreen(it) },
                selectDay = vm::selectDay,
                selectCategory = vm::selectCategory,
                toggle = vm::toggle,
            )
        }
        ScheduleContent(state, vm.isRefreshing.collectAsState().value, actions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleContent(
    state: State,
    isRefreshing: Boolean,
    actions: ScheduleActions,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        ),
        topBar = {
            val refresh = stringResource(MR.strings.action_refresh)
            Toolbar(
                stringResource(MR.strings.location_schedule),
                actions = { remember(refresh) { persistentListOf(ActionItem(refresh, Icons.Rounded.Refresh, doAction = actions.refresh)) } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                State.Loading -> LoadingScreen()
                is State.Failed -> ErrorScreen(state.message, retry = actions.refresh)
                is State.Loaded -> if (state.categories.isEmpty() && state.sections.isEmpty()) {
                    EmptyScreen(stringResource(MR.strings.schedule_empty))
                } else {
                    Column {
                        ScheduleFilterBar(
                            week = state.week,
                            selectedDay = state.selectedDay,
                            onSelectDay = actions.selectDay,
                            categories = state.categories,
                            selectedCategory = state.selectedCategory,
                            onSelectCategory = actions.selectCategory,
                        )
                        HorizontalDivider()
                        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = actions.refresh) {
                            if (state.sections.isEmpty()) {
                                EmptyScreen(stringResource(MR.strings.schedule_filtered_empty))
                            } else {
                                ScheduleList(state.sections, state.expanded, actions)
                            }
                        }
                    }
                }
            }
        }
    }
}

private class ScheduleActions(
    val refresh: () -> Unit,
    val open: (mangaId: Long) -> Unit,
    val selectDay: (LocalDate) -> Unit,
    val selectCategory: (Long?) -> Unit,
    val toggle: (ScheduleGroup) -> Unit,
)

@Composable
private fun ScheduleList(
    sections: List<ScheduleSection>,
    expanded: Set<ScheduleGroup>,
    actions: ScheduleActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.bottomNav.add(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)).asPaddingValues(),
    ) {
        sections.forEach { section ->
            val open = section.group in expanded
            stickyHeader(key = section.group.name) {
                SectionHeader(stringResource(section.group.title), section.series.size, open) { actions.toggle(section.group) }
            }
            if (open) {
                items(section.series, key = { "${section.group.name}-${it.manga.id}" }) { series ->
                    ScheduleRow(series, onClick = { actions.open(series.manga.id) })
                }
            }
        }
        item(key = "note") {
            Text(
                text = stringResource(MR.strings.schedule_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    open: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Badge(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) { Text(count.toString()) }
        Icon(
            imageVector = if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
