/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.history

import dev.bunko.domain.ServerListeners
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.model.ReadHistoryPage
import dev.bunko.domain.chapter.service.ChapterRepository
import dev.bunko.domain.global.model.GlobalMeta
import dev.bunko.domain.global.service.GlobalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

@Inject
class ReadHistory(
    private val chapterRepository: ChapterRepository,
    private val globalRepository: GlobalRepository,
    private val serverListeners: ServerListeners,
) {
    suspend fun clearedAt(): Result<Long> = attempt { globalRepository.getGlobalMeta().first().historyClearedAt }

    suspend fun page(
        clearedAt: Long,
        offset: Int,
        count: Int = PAGE_SIZE,
    ): Result<ReadHistoryPage> =
        attempt {
            val page = chapterRepository.getReadHistory(clearedAt, count, offset).first()
            page.copy(entries = page.entries.filter { it.chapter.isVisibleInHistory() })
        }

    suspend fun remove(chapter: Chapter): Result<Unit> =
        attempt {
            val readAt = chapter.lastReadAt ?: return@attempt
            chapterRepository.updateChapterMeta(chapter.id, Chapter.META_HISTORY_HIDDEN_AT, readAt.toString()).collect()
            serverListeners.updateManga(chapter.mangaId)
        }

    suspend fun clear(newestReadAt: Long): Result<Unit> =
        attempt { globalRepository.updateGlobalMeta(GlobalMeta.KEY_HISTORY_CLEARED_AT, newestReadAt.toString()).collect() }

    private inline fun <T> attempt(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    companion object {
        const val PAGE_SIZE = 50
    }
}

fun Chapter.isVisibleInHistory(): Boolean = (lastReadAt ?: 0L) > meta.historyHiddenAt
