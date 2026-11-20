/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.history

import androidx.compose.runtime.Immutable
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.ui.base.localDateOf
import kotlinx.datetime.LocalDate

@Immutable
data class HistoryEntry(
    val mangaTitle: String,
    val cover: Any?,
    val chapter: Chapter,
    val readAt: Long?,
    val pending: Boolean,
)

@Immutable
data class HistoryDay(
    val date: LocalDate,
    val entries: List<HistoryEntry>,
)

fun List<HistoryEntry>.groupByDay(): List<HistoryDay> =
    filter { it.readAt != null }
        .groupBy { localDateOf(it.readAt!! * 1000) }
        .map { (date, entries) -> HistoryDay(date, entries) }
