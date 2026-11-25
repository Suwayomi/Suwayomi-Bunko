/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.library.service

import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.schedule.ReleaseHistory
import dev.bunko.domain.library.model.LibraryStatistics
import dev.bunko.domain.library.model.UpdaterJobsInfo
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun addMangaToLibrary(mangaId: Long): Flow<Unit>

    fun removeMangaFromLibrary(mangaIds: List<Long>): Flow<Unit>

    fun getLibraryManga(): Flow<List<Manga>>

    fun libraryUpdateSubscription(): Flow<UpdaterJobsInfo>

    fun getReleaseHistory(since: Long): Flow<List<ReleaseHistory>>

    fun getStatistics(): Flow<LibraryStatistics>
}
