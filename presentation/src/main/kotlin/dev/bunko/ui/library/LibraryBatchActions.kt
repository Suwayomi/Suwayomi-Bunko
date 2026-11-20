/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library

import dev.bunko.domain.category.interactor.AddMangaToCategory
import dev.bunko.domain.category.interactor.GetMangaCategories
import dev.bunko.domain.category.interactor.RemoveMangaFromCategory
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.chapter.interactor.GetChapters
import dev.bunko.domain.chapter.interactor.UpdateChapter
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.download.interactor.BatchChapterDownload
import me.tatarka.inject.annotations.Inject

/** Chapter and category changes applied to several library entries at once. */
@Inject
class LibraryBatchActions(
    private val getChapters: GetChapters,
    private val updateChapter: UpdateChapter,
    private val batchChapterDownload: BatchChapterDownload,
    private val getMangaCategories: GetMangaCategories,
    private val addMangaToCategory: AddMangaToCategory,
    private val removeMangaFromCategory: RemoveMangaFromCategory,
) {
    suspend fun setRead(
        mangaIds: Set<Long>,
        read: Boolean,
        onError: suspend (Throwable) -> Unit,
    ) {
        val chapters = chaptersOf(mangaIds, onError).filter { it.read != read }
        if (chapters.isEmpty()) return
        updateChapter.await(chapters.map { it.id }, chapters.map { it.mangaId }.distinct(), read = read, onError = onError)
    }

    suspend fun downloadUnread(
        mangaIds: Set<Long>,
        onError: suspend (Throwable) -> Unit,
    ) {
        val ids = chaptersOf(mangaIds, onError).filter { !it.read && !it.downloaded }.map { it.id }
        if (ids.isNotEmpty()) batchChapterDownload.await(ids, onError)
    }

    /** Leaves each entry in exactly [categories]; an empty list means the default category. */
    suspend fun setCategories(
        mangaIds: Set<Long>,
        categories: List<Category>,
        onError: suspend (Throwable) -> Unit,
    ) {
        val wanted = categories.mapTo(HashSet()) { it.id }.ifEmpty { setOf(DEFAULT_CATEGORY) }
        mangaIds.forEach { mangaId ->
            val current = getMangaCategories.await(mangaId, onError)
                ?.mapTo(HashSet()) { it.id }
                ?.ifEmpty { setOf(DEFAULT_CATEGORY) }
                ?: return@forEach
            (current - wanted).forEach { removeMangaFromCategory.await(mangaId, it, onError) }
            (wanted - current).forEach { addMangaToCategory.await(mangaId, it, onError) }
        }
    }

    private suspend fun chaptersOf(
        mangaIds: Set<Long>,
        onError: suspend (Throwable) -> Unit,
    ): List<Chapter> = mangaIds.flatMap { getChapters.await(it, onError).orEmpty() }

    private companion object {
        const val DEFAULT_CATEGORY = 0L
    }
}
