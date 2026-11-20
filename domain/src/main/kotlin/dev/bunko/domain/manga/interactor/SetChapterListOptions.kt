/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.manga.interactor

import dev.bunko.domain.chapter.model.ChapterListOptions
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@Inject
class SetChapterListOptions(
    private val setMangaMetaValue: SetMangaMetaValue,
) {
    /** Stores [options] on the entry; null removes them so the entry follows the global default again. */
    suspend fun await(
        mangaId: Long,
        options: ChapterListOptions?,
        onError: suspend (Throwable) -> Unit = {},
    ) = setMangaMetaValue.await(
        mangaId = mangaId,
        key = ChapterListOptions.META_KEY,
        value = options?.let { Json.encodeToString(ChapterListOptions.serializer(), it) },
        onError = onError,
    )
}
