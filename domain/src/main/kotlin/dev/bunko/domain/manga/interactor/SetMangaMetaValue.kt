/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.manga.interactor

import dev.bunko.domain.ServerListeners
import dev.bunko.domain.manga.service.MangaRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject

@Inject
class SetMangaMetaValue(
    private val mangaRepository: MangaRepository,
    private val serverListeners: ServerListeners,
) {
    /** Stores [value] under [key] on the entry; null removes the key. */
    suspend fun await(
        mangaId: Long,
        key: String,
        value: String?,
        onError: suspend (Throwable) -> Unit = {},
    ) = flow {
        if (value == null) {
            mangaRepository.deleteMangaMeta(mangaId, key).collect()
        } else {
            mangaRepository.updateMangaMeta(mangaId, key, value).collect()
        }
        serverListeners.updateManga(mangaId)
        emit(Unit)
    }
        .catch {
            onError(it)
            log.warn(it) { "Failed to save $key of $mangaId" }
        }
        .collect()

    companion object {
        private val log = logging()
    }
}
