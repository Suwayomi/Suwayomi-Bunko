/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data

import dev.bunko.core.di.AppScope
import dev.bunko.data.backup.BackupRepositoryImpl
import dev.bunko.data.category.CategoryRepositoryImpl
import dev.bunko.data.chapter.ChapterRepositoryImpl
import dev.bunko.data.download.DownloadRepositoryImpl
import dev.bunko.data.extension.ExtensionRepositoryImpl
import dev.bunko.data.global.GlobalRepositoryImpl
import dev.bunko.data.library.LibraryRepositoryImpl
import dev.bunko.data.manga.MangaRepositoryImpl
import dev.bunko.data.settings.SettingsRepositoryImpl
import dev.bunko.data.source.SourceRepositoryImpl
import dev.bunko.data.track.TrackRepositoryImpl
import dev.bunko.data.updates.UpdatesRepositoryImpl
import dev.bunko.data.user.UserRepositoryImpl
import dev.bunko.data.util.ApolloAuthInterceptor
import dev.bunko.data.util.ApolloRetryInterceptor
import dev.bunko.data.util.KtorWebSocketEngine
import dev.bunko.data.util.NonClosingHttpEngine
import dev.bunko.domain.backup.service.BackupRepository
import dev.bunko.domain.category.service.CategoryRepository
import dev.bunko.domain.chapter.service.ChapterRepository
import dev.bunko.domain.download.service.DownloadRepository
import dev.bunko.domain.extension.service.ExtensionRepository
import dev.bunko.domain.global.service.GlobalRepository
import dev.bunko.domain.library.service.LibraryRepository
import dev.bunko.domain.manga.service.MangaRepository
import dev.bunko.domain.server.Http
import dev.bunko.domain.server.HttpNoAuth
import dev.bunko.domain.server.model.Auth
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.settings.service.SettingsRepository
import dev.bunko.domain.source.service.SourceRepository
import dev.bunko.domain.track.service.TrackRepository
import dev.bunko.domain.updates.service.UpdatesRepository
import dev.bunko.domain.user.interactor.UserRefreshUI
import dev.bunko.domain.user.service.UserRepository
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.network.websocket.GraphQLWsProtocol
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.apollographql.ktor.http.KtorHttpEngine
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Provides

typealias ApolloAppClient = StateFlow<ApolloClient>

typealias ApolloAppClientNoAuth = StateFlow<ApolloClient>

@OptIn(ApolloExperimental::class)
private fun getApolloClient(
    httpClient: HttpClient,
    serverUrl: Url,
    authInterceptor: ApolloAuthInterceptor?,
): ApolloClient {
    val url = URLBuilder(serverUrl)
        .appendPathSegments("api", "graphql")
        .buildString()
    return ApolloClient.Builder()
        .serverUrl(url)
        .httpEngine(NonClosingHttpEngine(KtorHttpEngine(httpClient)))
        .addInterceptor(ApolloRetryInterceptor)
        .apply {
            if (authInterceptor != null) {
                addInterceptor(authInterceptor)
            }
        }
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(url)
                .pingInterval(15.seconds)
                .webSocketEngine(KtorWebSocketEngine(httpClient))
                .wsProtocol(GraphQLWsProtocol(connectionPayload = { authInterceptor?.connectionPayload() }))
                .build()
        )
        .dispatcher(Dispatchers.IO)
        .build()
}

private val log = logging("ApolloClients")

@OptIn(DelicateCoroutinesApi::class)
private fun <K> StateFlow<K>.asApolloClientState(create: (K) -> ApolloClient): StateFlow<ApolloClient> {
    var currentKey = value
    val state = MutableStateFlow(create(currentKey))
    onEach { key ->
        if (key != currentKey) {
            currentKey = key
            val old = state.value
            state.value = create(key)
            runCatching { old.close() }.onFailure { log.warn(it) { "Failed to close previous ApolloClient" } }
        }
    }.launchIn(GlobalScope)
    return state.asStateFlow()
}

private data class AuthClientKey(
    val http: HttpClient,
    val auth: Auth,
    val username: String,
    val password: String,
)

interface DataComponent {

    @OptIn(DelicateCoroutinesApi::class)
    @Provides
    @AppScope
    fun apolloAppClient(
        http: HttpNoAuth,
        serverPreferences: ServerPreferences,
        apolloAuthInterceptor: ApolloAuthInterceptor,
    ): ApolloAppClient =
        combine(
            http,
            serverPreferences.auth().stateIn(GlobalScope),
            serverPreferences.authUsername().stateIn(GlobalScope),
            serverPreferences.authPassword().stateIn(GlobalScope),
            ::AuthClientKey,
        ).stateIn(
            GlobalScope,
            SharingStarted.Eagerly,
            AuthClientKey(
                http.value,
                serverPreferences.auth().get(),
                serverPreferences.authUsername().get(),
                serverPreferences.authPassword().get(),
            ),
        ).asApolloClientState { getApolloClient(it.http, serverPreferences.serverUrl().get(), apolloAuthInterceptor) }

    @Provides
    @AppScope
    fun apolloAppClientNoAuth(
        httpNoAuth: HttpNoAuth,
        serverPreferences: ServerPreferences,
        userRefreshUI: Lazy<UserRefreshUI>,
    ): ApolloAppClientNoAuth =
        httpNoAuth.asApolloClientState { getApolloClient(it, serverPreferences.serverUrl().get(), null) }

    @Provides
    fun settingsRepository(apolloAppClient: ApolloAppClient): SettingsRepository = SettingsRepositoryImpl(apolloAppClient)

    @Provides
    fun categoryRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
    ): CategoryRepository = CategoryRepositoryImpl(apolloAppClient, http)

    @Provides
    fun chapterRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
        serverPreferences: ServerPreferences,
    ): ChapterRepository = ChapterRepositoryImpl(apolloAppClient, http, serverPreferences.serverUrl())

    @Provides
    fun downloadRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
    ): DownloadRepository = DownloadRepositoryImpl(apolloAppClient, http)

    @Provides
    fun extensionRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
    ): ExtensionRepository = ExtensionRepositoryImpl(apolloAppClient, http)

    @Provides
    fun globalRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
    ): GlobalRepository = GlobalRepositoryImpl(apolloAppClient, http)

    @Provides
    fun libraryRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
    ): LibraryRepository = LibraryRepositoryImpl(apolloAppClient, http)

    @Provides
    fun mangaRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
        httpNoAuth: HttpNoAuth,
    ): MangaRepository = MangaRepositoryImpl(apolloAppClient, http, httpNoAuth)

    @Provides
    fun sourceRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
    ): SourceRepository = SourceRepositoryImpl(apolloAppClient, http)

    @Provides
    fun updatesRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
    ): UpdatesRepository = UpdatesRepositoryImpl(apolloAppClient, http)

    @Provides
    fun userRepository(
        apolloAppClient: ApolloAppClient,
        apolloAppClientNoAuth: ApolloAppClientNoAuth,
        http: Http,
        httpNoAuth: HttpNoAuth,
    ): UserRepository = UserRepositoryImpl(apolloAppClient, apolloAppClientNoAuth, http, httpNoAuth)

    @Provides
    fun trackRepository(apolloAppClient: ApolloAppClient): TrackRepository = TrackRepositoryImpl(apolloAppClient)

    @Provides
    fun backupRepository(
        apolloAppClient: ApolloAppClient,
        http: Http,
        serverPreferences: ServerPreferences,
    ): BackupRepository = BackupRepositoryImpl(apolloAppClient, http, serverPreferences.serverUrl())
}
