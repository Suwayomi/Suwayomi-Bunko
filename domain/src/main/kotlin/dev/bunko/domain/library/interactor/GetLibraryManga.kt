/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.library.interactor

import com.diamondedge.logging.logging
import dev.bunko.domain.library.service.LibraryRepository
import dev.bunko.domain.manga.model.Manga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.singleOrNull
import me.tatarka.inject.annotations.Inject

@Inject
class GetLibraryManga(
    private val libraryRepository: LibraryRepository,
) {
    suspend fun await(onError: suspend (Throwable) -> Unit = {}): List<Manga>? =
        asFlow()
            .catch {
                onError(it)
                log.warn(it) { "Failed to get library manga" }
            }
            .singleOrNull()

    fun asFlow(): Flow<List<Manga>> = libraryRepository.getLibraryManga()

    companion object {
        private val log = logging()
    }
}
