/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse

import dev.bunko.domain.category.interactor.AddMangaToCategory
import dev.bunko.domain.category.interactor.GetCategories
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.library.interactor.AddMangaToLibrary
import dev.bunko.domain.library.interactor.RemoveMangaFromLibrary
import dev.bunko.domain.manga.model.Manga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

/** Adds or removes a browsed title without opening it first. */
@Inject
class SourceLibraryActions(
    private val getCategories: GetCategories,
    private val addMangaToLibrary: AddMangaToLibrary,
    private val addMangaToCategory: AddMangaToCategory,
    private val removeMangaFromLibrary: RemoveMangaFromLibrary,
) {
    fun categories(
        scope: CoroutineScope,
        onError: suspend (Throwable) -> Unit,
    ): StateFlow<ImmutableList<Category>> =
        getCategories.asFlow(true)
            .map { it.toImmutableList() }
            .catch { onError(it) }
            .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    suspend fun add(
        manga: Manga,
        categories: List<Category>,
        onError: suspend (Throwable) -> Unit,
    ) {
        addMangaToLibrary.await(manga, onError) ?: return
        if (categories.isEmpty()) {
            addMangaToCategory.await(manga.id, 0, onError)
        } else {
            categories.forEach { addMangaToCategory.await(manga, it, onError) }
        }
    }

    suspend fun remove(
        manga: Manga,
        onError: suspend (Throwable) -> Unit,
    ) {
        removeMangaFromLibrary.await(listOf(manga.id), onError)
    }
}
