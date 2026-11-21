/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.category.interactor

import dev.bunko.domain.ServerListeners
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.category.service.CategoryRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take
import me.tatarka.inject.annotations.Inject

@Inject
class GetMangaListFromCategory(
    private val categoryRepository: CategoryRepository,
    private val serverListeners: ServerListeners,
) {
    suspend fun await(
        categoryId: Long,
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow(categoryId)
        .take(1)
        .catch {
            onError(it)
            log.warn(it) { "Failed to get manga list from category $categoryId" }
        }
        .singleOrNull()

    suspend fun await(
        category: Category,
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow(category)
        .take(1)
        .catch {
            onError(it)
            log.warn(it) { "Failed to get manga list from category ${category.name}" }
        }
        .singleOrNull()

    fun asFlow(categoryId: Long) =
        serverListeners.combineCategoryManga(
            categoryRepository.getMangaFromCategory(categoryId),
            predate = { categoryId == it },
        )

    fun asFlow(
        category: Category,
        holdsManga: (suspend (List<Long>) -> Boolean)? = null,
        cacheFirst: Boolean = false,
    ) = serverListeners.combineCategoryManga(
        categoryRepository.getMangaFromCategory(category.id),
        cached = if (cacheFirst) categoryRepository.getMangaFromCategory(category.id, cacheFirst = true) else null,
        predate = { category.id == it },
        holdsManga = holdsManga,
    )

    companion object {
        private val log = logging()
    }
}
