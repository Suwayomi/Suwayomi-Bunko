/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.library.interactor

import dev.bunko.domain.library.model.LibraryStatistics
import dev.bunko.domain.library.service.LibraryRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.singleOrNull
import me.tatarka.inject.annotations.Inject

@Inject
class GetLibraryStatistics(
    private val libraryRepository: LibraryRepository,
) {
    suspend fun await(onError: suspend (Throwable) -> Unit = {}): LibraryStatistics? =
        asFlow()
            .catch {
                onError(it)
                log.warn(it) { "Failed to get library statistics" }
            }
            .singleOrNull()

    fun asFlow(): Flow<LibraryStatistics> = libraryRepository.getStatistics()

    companion object {
        private val log = logging()
    }
}
