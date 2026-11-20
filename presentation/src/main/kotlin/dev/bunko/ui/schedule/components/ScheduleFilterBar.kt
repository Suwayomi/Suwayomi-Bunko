/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.base.today
import dev.bunko.ui.schedule.CategoryChip
import dev.bunko.ui.schedule.WeekDay
import dev.bunko.uicore.resources.stringResource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScheduleFilterBar(
    week: List<WeekDay>,
    selectedDay: LocalDate?,
    onSelectDay: (LocalDate) -> Unit,
    categories: List<CategoryChip>,
    selectedCategory: Long?,
    onSelectCategory: (Long?) -> Unit,
) {
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WeekStrip(week, selectedDay, onSelectDay)
        if (categories.size > 1) CategoryChips(categories, selectedCategory, onSelectCategory)
    }
}

@Composable
private fun WeekStrip(
    week: List<WeekDay>,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
) {
    val today = today()
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(week, key = { it.date.toString() }) { day ->
            DayCell(day, isToday = day.date == today, selected = day.date == selected, onClick = { onSelect(day.date) })
        }
    }
}

@Composable
private fun DayCell(
    day: WeekDay,
    isToday: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = day.releases > 0 || selected,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        modifier = Modifier.width(46.dp).alpha(if (day.releases > 0 || selected) 1f else 0.45f),
    ) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                shortDayName(day.date),
                style = MaterialTheme.typography.labelSmall,
                color = if (isToday && !selected) colors.primary else Color.Unspecified,
            )
            Text(day.date.day.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            ReleaseCount(day.releases, selected)
        }
    }
}

@Composable
private fun ReleaseCount(
    count: Int,
    selected: Boolean,
) {
    if (count == 0) {
        Spacer(Modifier.size(16.dp))
        return
    }
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(count.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<CategoryChip>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(stringResource(MR.strings.schedule_all)) })
        }
        items(categories, key = { it.id }) { chip ->
            FilterChip(
                selected = selected == chip.id,
                onClick = { onSelect(chip.id.takeUnless { it == selected }) },
                label = { Text("${chip.name} · ${chip.count}") },
            )
        }
    }
}

private fun shortDayName(date: LocalDate): String =
    java.time.DayOfWeek.of(date.dayOfWeek.isoDayNumber).getDisplayName(TextStyle.SHORT, Locale.getDefault())
