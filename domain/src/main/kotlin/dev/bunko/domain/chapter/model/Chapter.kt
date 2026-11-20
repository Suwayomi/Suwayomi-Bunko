/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.chapter.model

import androidx.compose.runtime.Immutable

@Immutable
data class Chapter(
    val id: Long,
    val url: String,
    val name: String,
    val uploadDate: Long,
    val chapterNumber: Float,
    val scanlator: String?,
    val mangaId: Long,
    val read: Boolean,
    val bookmarked: Boolean,
    val lastPageRead: Int,
    val index: Int,
    val fetchedAt: Long,
    val realUrl: String?,
    val pageCount: Int?,
    val lastReadAt: Long?,
    val downloaded: Boolean,
    val meta: ChapterMeta,
) {
    @Immutable
    data class ChapterMeta(
        val bunkoPageOffset: Int = 0,
        val historyHiddenAt: Long = 0,
    )

    companion object {
        const val META_HISTORY_HIDDEN_AT = "@@bunkoHistoryHiddenAt"
    }
}

