/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base

import androidx.compose.runtime.Composable
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun localDateOf(epochMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date

@Composable
fun relativeDate(date: LocalDate): String {
    val today = today()
    return when (today.daysUntil(date)) {
        0 -> stringResource(MR.strings.updates_today)
        -1 -> stringResource(MR.strings.updates_yesterday)
        1 -> stringResource(MR.strings.date_tomorrow)
        else -> {
            val day = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val year = if (date.year != today.year) ", ${date.year}" else ""
            "$day, $month ${date.day}$year"
        }
    }
}
