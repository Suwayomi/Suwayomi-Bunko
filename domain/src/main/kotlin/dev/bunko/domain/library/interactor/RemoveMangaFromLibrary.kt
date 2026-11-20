/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.library.interactor

import dev.bunko.domain.ServerListeners
import dev.bunko.domain.library.service.LibraryRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.singleOrNull
import me.tatarka.inject.annotations.Inject

@Inject
class RemoveMangaFromLibrary(
    private val libraryRepository: LibraryRepository,
    private val serverListeners: ServerListeners,
) {
    suspend fun await(
        mangaIds: List<Long>,
        onError: suspend (Throwable) -> Unit = {},
    ): Boolean {
        if (mangaIds.isEmpty()) return true
        return asFlow(mangaIds)
            .catch {
                onError(it)
                log.warn(it) { "Failed to remove $mangaIds from library" }
            }
            .singleOrNull() != null
    }

    fun asFlow(mangaIds: List<Long>) =
        libraryRepository.removeMangaFromLibrary(mangaIds)
            .onEach { serverListeners.updateManga(mangaIds) }

    companion object {
        private val log = logging()
    }
}
