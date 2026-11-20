/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data.global

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.GetGlobalMetaQuery
import dev.bunko.data.graphql.SetGlobalMetaMutation
import dev.bunko.domain.global.model.GlobalMeta
import dev.bunko.domain.global.service.GlobalRepository
import dev.bunko.domain.server.Http
import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GlobalRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
) : GlobalRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun getGlobalMeta(): Flow<GlobalMeta> =
        apolloClient.query(
            GetGlobalMetaQuery(),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                val metas = data.metas.nodes
                GlobalMeta(
                    example = metas.find { it.key == "example" }?.value?.toIntOrNull() ?: 0,
                    historyClearedAt = metas.find { it.key == GlobalMeta.KEY_HISTORY_CLEARED_AT }?.value?.toLongOrNull() ?: 0,
                )
            }

    override fun updateGlobalMeta(
        key: String,
        value: String,
    ): Flow<Unit> =
        apolloClient.mutation(
            SetGlobalMetaMutation(key, value),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.setGlobalMeta!!.clientMutationId
            }
}
