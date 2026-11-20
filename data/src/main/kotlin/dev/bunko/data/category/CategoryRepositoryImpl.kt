/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data.category

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.AddMangaToCategoriesMutation
import dev.bunko.data.graphql.CreateCategoryMutation
import dev.bunko.data.graphql.DeleteCategoryMutation
import dev.bunko.data.graphql.GetCategoriesQuery
import dev.bunko.data.graphql.GetCategoryMangaQuery
import dev.bunko.data.graphql.GetMangaCategoriesQuery
import dev.bunko.data.graphql.ModifyCategoryMutation
import dev.bunko.data.graphql.RemoveMangaFromCategoriesMutation
import dev.bunko.data.graphql.ReorderCategoryMutation
import dev.bunko.data.graphql.SetCategoryMetaMutation
import dev.bunko.data.graphql.fragment.CategoryFragment
import dev.bunko.data.manga.MangaRepositoryImpl.Companion.toManga
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.category.service.CategoryRepository
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.server.Http
import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
) : CategoryRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun getMangaCategories(mangaId: Long): Flow<List<Category>> =
        apolloClient.query(
            GetMangaCategoriesQuery(mangaId.toInt()),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.manga.categories.nodes.map { it.categoryFragment.toCategory() }
            }

    override fun addMangaToCategory(
        mangaId: Long,
        categoryId: Long,
    ): Flow<Unit> =
        apolloClient.mutation(
            AddMangaToCategoriesMutation(mangaId.toInt(), listOf(categoryId.toInt())),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.updateMangaCategories!!.clientMutationId
            }

    override fun removeMangaFromCategory(
        mangaId: Long,
        categoryId: Long,
    ): Flow<Unit> =
        apolloClient.mutation(
            RemoveMangaFromCategoriesMutation(mangaId.toInt(), listOf(categoryId.toInt())),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.updateMangaCategories!!.clientMutationId
            }

    override fun getCategories(): Flow<List<Category>> =
        apolloClient.query(
            GetCategoriesQuery(),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.categories.nodes
                    .filterNot { it.categoryFragment.id == 0 && it.categoryFragment.mangas.totalCount == 0 }
                    .map { it.categoryFragment.toCategory() }
            }

    override fun createCategory(name: String): Flow<Unit> =
        apolloClient.mutation(
            CreateCategoryMutation(name),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.createCategory!!.clientMutationId
            }

    override fun modifyCategory(
        categoryId: Long,
        name: String,
    ): Flow<Unit> =
        apolloClient.mutation(
            ModifyCategoryMutation(categoryId.toInt(), name),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.updateCategory!!.clientMutationId
            }

    override fun reorderCategory(
        categoryId: Long,
        position: Int,
    ): Flow<Unit> =
        apolloClient.mutation(
            ReorderCategoryMutation(categoryId.toInt(), position),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.updateCategoryOrder!!.clientMutationId
            }

    override fun deleteCategory(categoryId: Long): Flow<Unit> =
        apolloClient.mutation(
            DeleteCategoryMutation(categoryId.toInt()),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.deleteCategory!!.clientMutationId
            }

    override fun getMangaFromCategory(categoryId: Long): Flow<List<Manga>> =
        apolloClient.query(
            GetCategoryMangaQuery(categoryId.toInt()),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.category.mangas.nodes.map { it.libraryMangaFragment.toManga() }.filter { it.inLibrary }
            }

    override fun updateCategoryMeta(
        categoryId: Long,
        key: String,
        value: String,
    ): Flow<Unit> =
        apolloClient.mutation(
            SetCategoryMetaMutation(categoryId.toInt(), key, value),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.setCategoryMeta!!.clientMutationId
            }

    companion object {
        internal fun CategoryFragment.toCategory(): Category =
            Category(
                id = id.toLong(),
                order = order,
                name = name,
                default = default,
                meta = Category.CategoryMeta(),
                size = mangas.totalCount,
            )
    }
}
