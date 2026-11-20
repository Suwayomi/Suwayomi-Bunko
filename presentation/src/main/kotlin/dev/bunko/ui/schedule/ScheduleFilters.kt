/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.schedule

import androidx.compose.runtime.Immutable
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.schedule.ReleaseEstimate
import dev.bunko.domain.schedule.ScheduledSeries
import dev.bunko.ui.base.localDateOf
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Immutable
data class CategoryChip(
    val id: Long,
    val name: String,
    val count: Int,
)

@Immutable
data class WeekDay(
    val date: LocalDate,
    val releases: Int,
)

const val WEEK_DAYS = 7

fun categoryChips(
    series: List<ScheduledSeries>,
    categories: List<Category>,
): List<CategoryChip> =
    categories.sortedBy { it.order }.mapNotNull { category ->
        val count = series.count { category.id in it.categoryIds }
        if (count == 0) null else CategoryChip(category.id, category.name, count)
    }

fun List<ScheduledSeries>.inCategory(id: Long?): List<ScheduledSeries> = if (id == null) this else filter { id in it.categoryIds }

fun ScheduledSeries.expectedOn(): LocalDate? = (estimate as? ReleaseEstimate.Expected)?.at?.let(::localDateOf)

fun List<ScheduledSeries>.weekStrip(today: LocalDate): List<WeekDay> {
    val perDay = mapNotNull { it.expectedOn() }.groupingBy { it }.eachCount()
    return (0 until WEEK_DAYS).map { offset ->
        val date = today.plus(DatePeriod(days = offset))
        WeekDay(date, perDay[date] ?: 0)
    }
}
