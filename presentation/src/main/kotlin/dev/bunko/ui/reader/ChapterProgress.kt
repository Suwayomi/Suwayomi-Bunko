/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import dev.bunko.domain.chapter.model.Chapter

internal data class ChapterProgress(
    val chapterId: Long,
    val mangaId: Long,
    val read: Boolean? = null,
    val lastPageRead: Int? = null,
    val pageOffset: Int? = null,
) {
    fun encode(): String = listOf(chapterId, mangaId, read, lastPageRead, pageOffset).joinToString(SEPARATOR) { it?.toString().orEmpty() }

    companion object {
        private const val SEPARATOR = "\t"
        private const val FIELDS = 5

        fun decode(line: String): ChapterProgress? {
            val f = line.split(SEPARATOR)
            if (f.size != FIELDS) return null
            return runCatching {
                ChapterProgress(
                    chapterId = f[0].toLong(),
                    mangaId = f[1].toLong(),
                    read = f[2].toBooleanStrictOrNull(),
                    lastPageRead = f[3].toIntOrNull(),
                    pageOffset = f[4].toIntOrNull(),
                )
            }.getOrNull()
        }
    }
}

internal fun Chapter.withProgress(progress: ChapterProgress?): Chapter =
    if (progress == null) {
        this
    } else {
        copy(
            read = progress.read ?: read,
            lastPageRead = progress.lastPageRead ?: lastPageRead,
            meta = progress.pageOffset?.let { meta.copy(bunkoPageOffset = it) } ?: meta,
        )
    }
