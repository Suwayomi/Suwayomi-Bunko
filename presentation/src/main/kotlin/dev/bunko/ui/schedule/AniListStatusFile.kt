/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.schedule

import dev.bunko.domain.schedule.AniListStatus
import dev.bunko.domain.schedule.AniListStatusStore
import dev.bunko.domain.schedule.StoredStatus
import dev.bunko.ui.base.writeAtomic
import okio.FileSystem
import okio.Path

internal fun encodeStatuses(entries: Map<Long, StoredStatus>): String =
    entries.entries.joinToString("\n") { (id, entry) -> "$id ${entry.status?.name ?: "-"} ${entry.fetchedAt}" }

internal fun decodeStatuses(text: String): Map<Long, StoredStatus> =
    text.lineSequence()
        .mapNotNull { line ->
            val parts = line.split(' ')
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val fetchedAt = parts[2].toLongOrNull() ?: return@mapNotNull null
            val status = if (parts[1] == "-") null else AniListStatus.entries.firstOrNull { it.name == parts[1] } ?: return@mapNotNull null
            id to StoredStatus(status, fetchedAt)
        }
        .toMap()

internal class AniListStatusFile(
    private val file: Path,
    private val fs: FileSystem,
) : AniListStatusStore {
    override fun load(): Map<Long, StoredStatus> =
        runCatching { if (fs.exists(file)) decodeStatuses(fs.read(file) { readUtf8() }) else emptyMap() }.getOrDefault(emptyMap())

    override fun save(entries: Map<Long, StoredStatus>) = fs.writeAtomic(file) { writeUtf8(encodeStatuses(entries)) }
}
