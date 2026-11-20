/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.offline

import dev.bunko.ui.base.offline.OfflineChapterMeta
import java.io.File

class OfflineManga(
    val id: Long,
    val title: String,
    val cover: File?,
    val chapters: List<OfflineChapterMeta>,
)

fun List<OfflineChapterMeta>.groupByManga(coverOf: (mangaId: Long) -> File?): List<OfflineManga> =
    groupBy { it.chapter.mangaId }
        .map { (id, metas) ->
            OfflineManga(
                id = id,
                title = metas.first().mangaTitle.ifBlank { id.toString() },
                cover = coverOf(id),
                chapters = metas.sortedBy { it.chapter.index },
            )
        }
        .sortedBy { it.title.lowercase() }
