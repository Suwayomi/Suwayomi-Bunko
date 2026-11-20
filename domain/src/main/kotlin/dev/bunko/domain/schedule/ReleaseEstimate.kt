/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.schedule

import dev.bunko.domain.manga.model.MangaStatus
import kotlin.math.max

sealed interface ReleaseEstimate {
    data class Expected(
        val at: Long,
        val intervalDays: Int,
    ) : ReleaseEstimate

    data class Ended(
        val reason: EndReason,
        val lastUpload: Long?,
    ) : ReleaseEstimate

    data class Unknown(
        val lastUpload: Long?,
    ) : ReleaseEstimate
}

enum class EndReason { Completed, Cancelled, Hiatus, Stalled }

enum class AniListStatus { Releasing, Finished, Hiatus, Cancelled, NotYetReleased }

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val RECENT_RELEASES = 10
private const val MIN_RELEASES = 3
private const val STALL_FACTOR = 2
private const val STALL_MIN_DAYS = 45

fun estimateRelease(
    history: ReleaseHistory,
    aniList: AniListStatus?,
    now: Long,
): ReleaseEstimate {
    val lastUpload = history.latestUpload ?: history.uploadDates.maxOrNull()
    endReason(history.manga.status, aniList)?.let { return ReleaseEstimate.Ended(it, lastUpload) }

    val releaseDays = history.uploadDates.filter { it > 0 }.map { it / DAY_MS }.distinct().sorted().takeLast(RECENT_RELEASES)
    if (releaseDays.size < MIN_RELEASES || lastUpload == null) return ReleaseEstimate.Unknown(lastUpload)

    val interval = max(1, median(releaseDays.zipWithNext { a, b -> (b - a).toInt() }))
    val silentDays = ((now - lastUpload) / DAY_MS).toInt()
    if (silentDays > max(interval * STALL_FACTOR, STALL_MIN_DAYS)) return ReleaseEstimate.Ended(EndReason.Stalled, lastUpload)

    return ReleaseEstimate.Expected(at = lastUpload + interval * DAY_MS, intervalDays = interval)
}

private fun endReason(
    status: MangaStatus,
    aniList: AniListStatus?,
): EndReason? =
    when (aniList) {
        AniListStatus.Finished -> EndReason.Completed
        AniListStatus.Cancelled -> EndReason.Cancelled
        AniListStatus.Hiatus -> EndReason.Hiatus
        AniListStatus.Releasing, AniListStatus.NotYetReleased -> null
        null -> when (status) {
            MangaStatus.COMPLETED, MangaStatus.PUBLISHING_FINISHED -> EndReason.Completed
            MangaStatus.CANCELLED -> EndReason.Cancelled
            MangaStatus.ON_HIATUS -> EndReason.Hiatus
            MangaStatus.UNKNOWN, MangaStatus.ONGOING, MangaStatus.LICENSED -> null
        }
    }

private fun median(values: List<Int>): Int {
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}
