/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data.user

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.ApolloAppClientNoAuth
import dev.bunko.data.graphql.LoginMutation
import dev.bunko.data.graphql.RefreshAccessTokenMutation
import dev.bunko.domain.server.Http
import dev.bunko.domain.server.HttpNoAuth
import dev.bunko.domain.user.model.LoginData
import dev.bunko.domain.user.service.UserRepository
import com.apollographql.apollo.ApolloClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val apolloAppClientNoAuth: ApolloAppClientNoAuth,
    private val http: Http,
    private val httpNoAuth: HttpNoAuth,
) : UserRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    val apolloClientNoAuth: ApolloClient
        get() = apolloAppClientNoAuth.value

    override fun loginUI(username: String, password: String): Flow<LoginData> {
        return apolloClientNoAuth.mutation(
            LoginMutation(username, password),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors.login
                LoginData(data.refreshToken, data.accessToken)
            }
    }

    override fun refreshUI(refreshToken: String): Flow<String> {
        return apolloClientNoAuth.mutation(
            RefreshAccessTokenMutation(refreshToken),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors.refreshToken
                data.accessToken
            }
    }

    override fun loginSimple(username: String, password: String): Flow<String> {
        return flow {
            val cookie = httpNoAuth.value.post(
                "/login.html",
            ) {
                formData {
                    append("user", username)
                    append("pass", password)
                }
            }.headers["Set-Cookie"]

            cookie ?: throw NullPointerException("Missing Set-Cookie header")

            emit(cookie.substringAfter("JSESSIONID=").substringBefore(";"))
        }
    }
}
