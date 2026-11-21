/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.statistics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bunko.domain.library.model.LibraryStatistics
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.userMessage
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.statistics.StatisticsState
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadStateCrossfade
import dev.bunko.uicore.components.SkeletonBlock
import dev.bunko.uicore.components.SkeletonHost
import dev.bunko.uicore.resources.stringResource
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatisticsScreenContent(
    state: StatisticsState,
    onRetry: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        ),
        topBar = { Toolbar(stringResource(MR.strings.location_statistics)) },
    ) { padding ->
        LoadStateCrossfade(state, Modifier.padding(padding).fillMaxSize()) { current ->
            when (current) {
                StatisticsState.Loading -> StatisticsSkeleton()
                is StatisticsState.Failed -> ErrorScreen(stringResource(current.e.userMessage), retry = onRetry)
                is StatisticsState.Loaded -> StatisticsList(current.statistics)
            }
        }
    }
}

@Composable
private fun StatisticsList(statistics: LibraryStatistics) {
    val number = NumberFormat.getIntegerInstance()
    fun count(value: Int) = number.format(value)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.bottomNav.add(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .asPaddingValues(),
    ) {
        item {
            StatisticsSection(stringResource(MR.strings.statistics_overview)) {
                OverviewValue(
                    count(statistics.inLibrary),
                    stringResource(MR.strings.statistics_in_library),
                    Icons.Outlined.CollectionsBookmark,
                )
                OverviewValue(
                    count(statistics.started),
                    stringResource(MR.strings.statistics_started),
                    Icons.AutoMirrored.Outlined.MenuBook,
                )
                OverviewValue(
                    count(statistics.completed),
                    stringResource(MR.strings.statistics_completed),
                    Icons.Outlined.DoneAll,
                )
            }
        }
        item {
            StatisticsSection(stringResource(MR.strings.statistics_entries)) {
                StatValue(count(statistics.inGlobalUpdate), stringResource(MR.strings.statistics_in_global_update))
                StatValue(count(statistics.local), stringResource(MR.strings.statistics_local))
            }
        }
        item {
            StatisticsSection(stringResource(MR.strings.statistics_chapters)) {
                StatValue(count(statistics.totalChapters), stringResource(MR.strings.statistics_total))
                StatValue(count(statistics.readChapters), stringResource(MR.strings.statistics_read))
                StatValue(count(statistics.downloadedChapters), stringResource(MR.strings.statistics_downloaded))
            }
        }
        item {
            StatisticsSection(stringResource(MR.strings.statistics_trackers)) {
                StatValue(count(statistics.trackedEntries), stringResource(MR.strings.statistics_tracked))
                StatValue(
                    statistics.meanScore?.let { String.format(Locale.getDefault(), "%.2f ★", it) } ?: "–",
                    stringResource(MR.strings.statistics_mean_score),
                )
                StatValue(count(statistics.trackersUsed), stringResource(MR.strings.statistics_trackers_used))
            }
        }
    }
}

private val CardShape = RoundedCornerShape(20.dp)

@Composable
private fun StatisticsSection(
    title: String,
    content: @Composable RowScope.() -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                content = content,
            )
        }
    }
}

@Composable
private fun RowScope.OverviewValue(
    value: String,
    label: String,
    icon: ImageVector,
) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp).size(24.dp),
        )
    }
}

@Composable
private fun RowScope.StatValue(
    value: String,
    label: String,
) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatisticsSkeleton() {
    SkeletonHost(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf(132.dp, 76.dp, 76.dp, 76.dp).forEach { height ->
                SkeletonBlock(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp).width(96.dp).height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth().height(height), shape = CardShape)
            }
        }
    }
}
