/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.manga.service

import dev.bunko.domain.manga.model.Manga
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow

interface MangaRepository {
    fun getManga(mangaId: Long): Flow<Manga>

    fun refreshManga(mangaId: Long): Flow<Manga>

    fun getMangaLibrary(mangaId: Long): Flow<Manga>

    fun getMangaThumbnail(
        mangaId: Long,
        block: HttpRequestBuilder.() -> Unit,
    ): Flow<ByteReadChannel>

    fun updateMangaMeta(
        mangaId: Long,
        key: String,
        value: String,
    ): Flow<Unit>

    fun deleteMangaMeta(
        mangaId: Long,
        key: String,
    ): Flow<Unit>
}
