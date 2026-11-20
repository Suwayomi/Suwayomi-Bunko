/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data.updates

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.chapter.ChapterRepositoryImpl.Companion.toMangaAndChapter
import dev.bunko.data.graphql.GetChapterUpdatesQuery
import dev.bunko.data.graphql.UpdateCategoryMutation
import dev.bunko.data.graphql.UpdateLibraryMutation
import dev.bunko.domain.server.Http
import dev.bunko.domain.updates.model.Updates
import dev.bunko.domain.updates.service.UpdatesRepository
import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UpdatesRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
) : UpdatesRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun getRecentUpdates(pageNum: Int): Flow<Updates> =
        apolloClient.query(
            GetChapterUpdatesQuery(50, pageNum * 50),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                Updates(
                    data.chapters.nodes.map { it.chapterWithMangaFragment.toMangaAndChapter() },
                    data.chapters.pageInfo.hasNextPage,
                )
            }

    override fun updateLibrary(): Flow<Unit> =
        apolloClient.mutation(
            UpdateLibraryMutation(),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.updateLibraryManga!!.clientMutationId
            }

    override fun updateCategory(categoryId: Long): Flow<Unit> =
        apolloClient.mutation(
            UpdateCategoryMutation(listOf(categoryId.toInt())),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.updateCategoryManga!!.clientMutationId
            }
}
