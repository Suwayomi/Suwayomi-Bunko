/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.chapter.interactor

import dev.bunko.domain.ServerListeners
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.service.ChapterRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import me.tatarka.inject.annotations.Inject

@Inject
class UpdateChapterMeta(
    private val chapterRepository: ChapterRepository,
    private val serverListeners: ServerListeners,
) {
    suspend fun await(
        chapter: Chapter,
        pageOffset: Int = chapter.meta.bunkoPageOffset,
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow(chapter, pageOffset)
        .catch {
            onError(it)
            log.warn(it) { "Failed to update ${chapter.name}(${chapter.index}) meta" }
        }
        .collect()

    fun asFlow(
        chapter: Chapter,
        pageOffset: Int = chapter.meta.bunkoPageOffset,
    ) = if (pageOffset != chapter.meta.bunkoPageOffset) asFlow(chapter.id, chapter.mangaId, pageOffset) else flowOf(Unit)

    fun asFlow(
        chapterId: Long,
        mangaId: Long,
        pageOffset: Int,
    ) = flow {
        chapterRepository.updateChapterMeta(
            chapterId,
            "@@bunkoPageOffset",
            pageOffset.toString(),
        ).collect()
        serverListeners.updateManga(mangaId)
        emit(Unit)
    }

    companion object {
        private val log = logging()
    }
}
