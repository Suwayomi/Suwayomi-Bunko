/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.server

import dev.bunko.core.prefs.Preference
import dev.bunko.domain.BuildConfig
import dev.bunko.domain.server.model.Auth
import dev.bunko.domain.server.model.Proxy
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.user.interactor.UserRefreshUI
import dev.bunko.domain.user.service.UserPreferences
import com.diamondedge.logging.logging
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.encodeBase64
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.io.IOException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

typealias Http = StateFlow<HttpClient>

typealias HttpNoAuth = StateFlow<HttpClient>

private val log = logging()

private val RETRYABLE_STATUS_CODES = setOf(502, 503, 504)

private class SimpleAuthPluginConfig {
    var simpleSessionPreference: Preference<String>? = null
    var userRefreshUI: Lazy<UserRefreshUI>? = null
    var trustedHosts: Set<String> = emptySet()
}

private fun HttpStatusCode.isAuthFailure() = this == HttpStatusCode.Unauthorized || this == HttpStatusCode.Forbidden

private fun HttpMethod.isIdempotent() = this == HttpMethod.Get || this == HttpMethod.Head || this == HttpMethod.Options

private val SimpleAuthPlugin = createClientPlugin("SimpleAuthPlugin", ::SimpleAuthPluginConfig) {
    val simpleSessionPreference = pluginConfig.simpleSessionPreference!!
    val userRefreshUI = pluginConfig.userRefreshUI!!
    val trustedHosts = pluginConfig.trustedHosts

    on(Send) { request ->
        if (request.url.host !in trustedHosts) {
            request.headers.remove("Cookie")
            return@on proceed(request)
        }
        val session = simpleSessionPreference.get()
        if (session.isNotEmpty()) {
            request.headers["Cookie"] = "JSESSIONID=$session"
        }
        val originalCall = proceed(request)
        if (originalCall.response.status.isAuthFailure()) {
            log.warn { "Session rejected (${originalCall.response.status.value}), renewing..." }
            val newSession = userRefreshUI.value.recoverSimple(session)
            if (newSession != null) {
                request.headers["Cookie"] = "JSESSIONID=$newSession"
                return@on proceed(request)
            }
        }
        originalCall
    }
}

private class BasicAuthPluginConfig {
    var header: String = ""
    var trustedHosts: Set<String> = emptySet()
}

// ktor's basic provider answers any host's 401 challenge; this one never sends the password off-server
private val BasicAuthPlugin = createClientPlugin("BasicAuth", ::BasicAuthPluginConfig) {
    val header = pluginConfig.header
    val trustedHosts = pluginConfig.trustedHosts

    on(Send) { request ->
        if (request.url.host in trustedHosts) {
            request.headers[HttpHeaders.Authorization] = header
        } else {
            request.headers.remove(HttpHeaders.Authorization)
        }
        proceed(request)
    }
}

private class UIAuthPluginConfig {
    var uiAccessTokenPreference: Preference<String>? = null
    var userRefreshUI: Lazy<UserRefreshUI>? = null
    var trustedHosts: Set<String> = emptySet()
}

private val UIAuthPlugin = createClientPlugin("UIAuthPlugin", ::UIAuthPluginConfig) {
    val uiAccessTokenPreference = pluginConfig.uiAccessTokenPreference!!
    val userRefreshUI = pluginConfig.userRefreshUI!!
    val trustedHosts = pluginConfig.trustedHosts

    on(Send) { request ->
        if (request.url.host !in trustedHosts) {
            request.headers.remove("Authorization")
            return@on proceed(request)
        }
        val token = uiAccessTokenPreference.get()
        if (token.isEmpty()) return@on proceed(request)

        request.headers["Authorization"] = "Bearer $token"
        val originalCall = proceed(request)
        if (originalCall.response.status.isAuthFailure()) {
            log.warn { "Token rejected (${originalCall.response.status.value}), refreshing..." }
            val newToken = userRefreshUI.value.recoverUI(token)
            if (newToken != null) {
                request.headers["Authorization"] = "Bearer $newToken"
                return@on proceed(request)
            }
        }
        originalCall
    }
}

private data class HttpConfig(
    val serverUrl: Url,
    val fallbackServer: String,
    val proxy: Proxy,
    val proxyHttpHost: String,
    val proxyHttpPort: Int,
    val proxySocksHost: String,
    val proxySocksPort: Int,
    val auth: Auth,
    val authUsername: String,
    val authPassword: String,
)

private fun HttpConfig.trustedHosts(): Set<String> =
    setOfNotNull(
        serverUrl.host,
        parseFallbackOrigin(fallbackServer, serverUrl.protocol.name, serverUrl.port)?.host,
    )

private fun getHttpClient(
    settings: HttpConfig,
    uiAccessTokenPreference: Preference<String>,
    simpleSessionPreference: Preference<String>,
    userRefreshUI: Lazy<UserRefreshUI>,
    json: Json
): HttpClient {
    val (serverUrl, fallbackServer, proxy, proxyHttpHost, proxyHttpPort, proxySocksHost, proxySocksPort, auth, authUsername, authPassword) =
        settings
    return HttpClient(OkHttp) {
        expectSuccess = true

        defaultRequest {
            url(serverUrl.toString())
        }

        engine {
            config {
                retryOnConnectionFailure(true)
                connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
                val primary = serverUrl.toString().toHttpUrlOrNull()
                val fallback = parseFallbackOrigin(fallbackServer, serverUrl.protocol.name, serverUrl.port)
                if (primary != null && fallback != null && !fallback.sameOrigin(primary.origin())) {
                    addInterceptor(ServerFailover(primary.origin(), fallback))
                }
            }
            this.proxy = when (proxy) {
                Proxy.NO_PROXY -> null

                Proxy.HTTP_PROXY -> ProxyBuilder.http(
                    URLBuilder(
                        host = proxyHttpHost,
                        port = proxyHttpPort,
                    ).build(),
                )

                Proxy.SOCKS_PROXY -> ProxyBuilder.socks(
                    proxySocksHost,
                    proxySocksPort,
                )
            }
        }
        when (auth) {
            Auth.BASIC -> install(BasicAuthPlugin) {
                this.header = "Basic " + "$authUsername:$authPassword".encodeBase64()
                this.trustedHosts = settings.trustedHosts()
            }

            Auth.SIMPLE -> install(SimpleAuthPlugin) {
                this.simpleSessionPreference = simpleSessionPreference
                this.userRefreshUI = userRefreshUI
                this.trustedHosts = settings.trustedHosts()
            }
            Auth.UI -> install(UIAuthPlugin) {
                this.uiAccessTokenPreference = uiAccessTokenPreference
                this.userRefreshUI = userRefreshUI
                this.trustedHosts = settings.trustedHosts()
            }

            else -> Unit
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10.seconds.inWholeMilliseconds
            requestTimeoutMillis = 60.seconds.inWholeMilliseconds
            socketTimeoutMillis = 45.seconds.inWholeMilliseconds
        }
        install(HttpRequestRetry) {
            maxRetries = 2
            retryOnExceptionIf { request, cause ->
                cause !is ConnectTimeoutException && request.method.isIdempotent() && (
                    cause is IOException ||
                        cause is SocketTimeoutException ||
                        cause is HttpRequestTimeoutException
                    )
            }
            retryIf { request, response ->
                request.method.isIdempotent() && response.status.value in RETRYABLE_STATUS_CODES
            }
            exponentialDelay(base = 2.0, maxDelayMs = 4_000, randomizationMs = 250)
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets) {
            pingIntervalMillis = 15.seconds.inWholeMilliseconds
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) {
                LogLevel.HEADERS
            } else {
                LogLevel.INFO
            }
            logger = object : Logger {
                val log = logging("HttpClient")

                override fun log(message: String) {
                    log.info { message }
                }
            }
        }
    }
}

private val REPLACED_CLIENT_GRACE = 30.seconds

@OptIn(DelicateCoroutinesApi::class)
private fun ServerPreferences.httpConfig(withAuth: Boolean): StateFlow<HttpConfig> {
    val connection = listOf(serverUrl(), fallbackServer(), proxy(), proxyHttpHost(), proxyHttpPort(), proxySocksHost(), proxySocksPort())
    val credentials = if (withAuth) listOf(auth(), authUsername(), authPassword()) else emptyList()
    val prefs = connection + credentials
    return combine(prefs.map { it.stateIn(GlobalScope) }) { it.toList().toHttpConfig() }
        .stateIn(GlobalScope, SharingStarted.Eagerly, prefs.map { it.get() }.toHttpConfig())
}

private fun List<Any?>.toHttpConfig() =
    HttpConfig(
        serverUrl = get(0) as Url,
        fallbackServer = get(1) as String,
        proxy = get(2) as Proxy,
        proxyHttpHost = get(3) as String,
        proxyHttpPort = get(4) as Int,
        proxySocksHost = get(5) as String,
        proxySocksPort = get(6) as Int,
        auth = getOrNull(7) as Auth? ?: Auth.NONE,
        authUsername = getOrNull(8) as String? ?: "",
        authPassword = getOrNull(9) as String? ?: "",
    )

@OptIn(DelicateCoroutinesApi::class)
private fun StateFlow<HttpConfig>.asHttpClientState(create: (HttpConfig) -> HttpClient): Http {
    var current = value
    val state = MutableStateFlow(create(current))
    onEach { config ->
        if (config != current) {
            current = config
            val old = state.value
            state.value = create(config)
            GlobalScope.launch {
                delay(REPLACED_CLIENT_GRACE)
                old.close()
            }
        }
    }.launchIn(GlobalScope)
    return state.asStateFlow()
}

fun httpClient(
    serverPreferences: ServerPreferences,
    userPreferences: UserPreferences,
    userRefreshUI: Lazy<UserRefreshUI>,
    json: Json,
): Http = serverPreferences.httpConfig(withAuth = true).asHttpClientState {
    getHttpClient(it, userPreferences.uiAccessToken(), userPreferences.simpleSession(), userRefreshUI, json)
}

fun httpClientNoAuth(
    serverPreferences: ServerPreferences,
    userPreferences: UserPreferences,
    userRefreshUI: Lazy<UserRefreshUI>,
    json: Json,
): HttpNoAuth = serverPreferences.httpConfig(withAuth = false).asHttpClientState {
    getHttpClient(it, userPreferences.uiAccessToken(), userPreferences.simpleSession(), userRefreshUI, json)
}
