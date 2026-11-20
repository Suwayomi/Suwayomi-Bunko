/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.chapter.interactor

import dev.bunko.domain.ServerListeners
import dev.bunko.domain.chapter.service.ChapterRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import me.tatarka.inject.annotations.Inject

@Inject
class GetLastReadChapter(
    private val chapterRepository: ChapterRepository,
    private val serverListeners: ServerListeners,
) {
    fun asFlow() = serverListeners.combineMangaUpdates(
        chapterRepository.getLastRead().catch {
            log.warn(it) { "Failed to get the last read chapter" }
            emit(null)
        },
    )

    private companion object {
        private val log = logging()
    }
}
