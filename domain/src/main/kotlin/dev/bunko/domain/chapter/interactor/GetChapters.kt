/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.chapter.interactor

import dev.bunko.domain.ServerListeners
import dev.bunko.domain.chapter.service.ChapterRepository
import dev.bunko.domain.manga.model.Manga
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take
import me.tatarka.inject.annotations.Inject

@Inject
class GetChapters(
    private val chapterRepository: ChapterRepository,
    private val serverListeners: ServerListeners,
) {
    suspend fun await(
        mangaId: Long,
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow(mangaId)
        .take(1)
        .catch {
            onError(it)
            log.warn(it) { "Failed to get chapters for $mangaId" }
        }
        .singleOrNull()

    suspend fun await(
        manga: Manga,
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow(manga)
        .take(1)
        .catch {
            onError(it)
            log.warn(it) { "Failed to get chapters for ${manga.title}(${manga.id})" }
        }
        .singleOrNull()

    fun asFlow(mangaId: Long) =
        serverListeners.combineMangaUpdates(
            chapterRepository.getChapters(mangaId),
            predate = { ids -> mangaId in ids },
        )

    fun asFlow(manga: Manga) =
        serverListeners.combineMangaUpdates(
            chapterRepository.getChapters(manga.id),
            predate = { ids -> manga.id in ids },
        )

    companion object {
        private val log = logging()
    }
}
