/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.schedule

import androidx.compose.runtime.Immutable
import dev.bunko.domain.schedule.ReleaseEstimate
import dev.bunko.domain.schedule.ScheduledSeries
import dev.bunko.i18n.MR
import dev.icerock.moko.resources.StringResource

enum class ScheduleGroup(
    val title: StringResource,
) {
    DueSoon(MR.strings.schedule_due_soon),
    NextWeek(MR.strings.schedule_next_week),
    Later(MR.strings.schedule_later),
    Ended(MR.strings.schedule_ended),
    Unknown(MR.strings.schedule_unknown),
}

@Immutable
data class ScheduleSection(
    val group: ScheduleGroup,
    val series: List<ScheduledSeries>,
)

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val WEEK_MS = 7 * DAY_MS

fun List<ScheduledSeries>.toSections(now: Long): List<ScheduleSection> =
    groupBy { it.group(now) }
        .map { (group, series) -> ScheduleSection(group, series.sortedWith(order)) }
        .sortedBy { it.group.ordinal }

private fun ScheduledSeries.group(now: Long): ScheduleGroup =
    when (val e = estimate) {
        is ReleaseEstimate.Expected -> when {
            e.at < now + WEEK_MS -> ScheduleGroup.DueSoon
            e.at < now + 2 * WEEK_MS -> ScheduleGroup.NextWeek
            else -> ScheduleGroup.Later
        }
        is ReleaseEstimate.Ended -> ScheduleGroup.Ended
        is ReleaseEstimate.Unknown -> ScheduleGroup.Unknown
    }

private val order = compareBy<ScheduledSeries>(
    { (it.estimate as? ReleaseEstimate.Expected)?.at ?: Long.MAX_VALUE },
    { it.manga.title.lowercase() },
)
