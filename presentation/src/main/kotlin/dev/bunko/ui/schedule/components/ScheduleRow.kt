/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.schedule.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.schedule.EndReason
import dev.bunko.domain.schedule.ReleaseEstimate
import dev.bunko.domain.schedule.ScheduledSeries
import dev.bunko.i18n.MR
import dev.bunko.ui.base.localDateOf
import dev.bunko.ui.base.relativeDate
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.uicore.components.MangaListItemImage
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.resources.stringResource
import kotlin.time.Clock

private const val DAY_MS = 24L * 60 * 60 * 1000

@Composable
fun ScheduleRow(
    series: ScheduledSeries,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaListItemImage(
            modifier = Modifier.height(64.dp).aspectRatio(mangaAspectRatio).libraryCoverFrame(),
            data = series.manga,
            contentDescription = series.manga.title,
        )
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = series.manga.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val (headline, color) = headline(series.estimate)
            Text(headline, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            detail(series.estimate)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun headline(estimate: ReleaseEstimate): Pair<String, Color> =
    when (estimate) {
        is ReleaseEstimate.Expected -> {
            val date = relativeDate(localDateOf(estimate.at))
            if (estimate.at < Clock.System.now().toEpochMilliseconds() - DAY_MS) {
                stringResource(MR.strings.schedule_overdue, date) to MaterialTheme.colorScheme.tertiary
            } else {
                stringResource(MR.strings.schedule_expected, date) to MaterialTheme.colorScheme.primary
            }
        }
        is ReleaseEstimate.Ended -> {
            val text = when (estimate.reason) {
                EndReason.Completed -> stringResource(MR.strings.schedule_completed)
                EndReason.Cancelled -> stringResource(MR.strings.schedule_cancelled)
                EndReason.Hiatus -> stringResource(MR.strings.schedule_hiatus)
                EndReason.Stalled -> {
                    val days = estimate.lastUpload?.let { ((Clock.System.now().toEpochMilliseconds() - it) / DAY_MS).toInt() } ?: 0
                    stringResource(MR.strings.schedule_stalled, days)
                }
            }
            text to MaterialTheme.colorScheme.onSurfaceVariant
        }
        is ReleaseEstimate.Unknown -> stringResource(MR.strings.schedule_unknown) to MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun detail(estimate: ReleaseEstimate): String? {
    val lastUpload = when (estimate) {
        is ReleaseEstimate.Expected -> null
        is ReleaseEstimate.Ended -> estimate.lastUpload
        is ReleaseEstimate.Unknown -> estimate.lastUpload ?: return stringResource(MR.strings.schedule_no_dates)
    }
    val cadence = (estimate as? ReleaseEstimate.Expected)?.let { cadence(it.intervalDays) }
    val last = lastUpload?.let { stringResource(MR.strings.schedule_last_upload, relativeDate(localDateOf(it))) }
    return listOfNotNull(cadence, last).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun cadence(days: Int): String =
    when (days) {
        1 -> stringResource(MR.strings.schedule_every_day)
        7 -> stringResource(MR.strings.schedule_every_week)
        else -> stringResource(MR.strings.schedule_every_days, days)
    }
