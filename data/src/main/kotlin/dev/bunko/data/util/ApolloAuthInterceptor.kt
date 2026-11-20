package dev.bunko.data.util

import dev.bunko.domain.server.model.Auth
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.user.interactor.UserRefreshUI
import dev.bunko.domain.user.service.UserPreferences
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloWebSocketClosedException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject
import kotlin.io.encoding.Base64

@Inject
class ApolloAuthInterceptor(
    private val userPreferences: UserPreferences,
    private val serverPreferences: ServerPreferences,
    private val userRefreshUI: Lazy<UserRefreshUI>
) : ApolloInterceptor {
    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain,
    ): Flow<ApolloResponse<D>> {
        val auth = serverPreferences.auth().get()
        val usedCredential = credential(auth)
        return flow {
            var recovered = false
            chain.proceed(request.withAuth(auth)).collect { response ->
                if (!recovered && response.isAuthFailure() && recover(auth, usedCredential)) {
                    log.warn { "${request.operation.name()} - Auth rejected, retrying with a renewed session" }
                    recovered = true
                    emitAll(chain.proceed(request.withAuth(auth)))
                } else {
                    emit(response)
                }
            }
        }
    }

    fun connectionPayload(): Map<String, String>? {
        if (serverPreferences.auth().get() != Auth.UI) return null
        val token = userPreferences.uiAccessToken().get()
        return token.takeIf { it.isNotEmpty() }?.let { mapOf("Authorization" to it) }
    }

    private fun credential(auth: Auth): String = when (auth) {
        Auth.UI -> userPreferences.uiAccessToken().get()
        Auth.SIMPLE -> userPreferences.simpleSession().get()
        else -> ""
    }

    private suspend fun recover(auth: Auth, usedCredential: String): Boolean = when (auth) {
        Auth.UI -> userRefreshUI.value.recoverUI(usedCredential) != null
        Auth.SIMPLE -> userRefreshUI.value.recoverSimple(usedCredential) != null
        else -> false
    }

    private fun <D : Operation.Data> ApolloRequest<D>.withAuth(auth: Auth): ApolloRequest<D> {
        val header = when (auth) {
            Auth.NONE, Auth.DIGEST -> null
            Auth.BASIC -> {
                val username = serverPreferences.authUsername().get()
                val password = serverPreferences.authPassword().get()
                HttpHeader("Authorization", "Basic ${Base64.encode("$username:$password".encodeToByteArray())}")
            }
            Auth.SIMPLE -> userPreferences.simpleSession().get()
                .takeIf { it.isNotEmpty() }
                ?.let { HttpHeader("Cookie", "JSESSIONID=$it") }
            Auth.UI -> userPreferences.uiAccessToken().get()
                .takeIf { it.isNotEmpty() }
                ?.let { HttpHeader("Authorization", "Bearer $it") }
        } ?: return this

        return newBuilder()
            .httpHeaders(
                httpHeaders.orEmpty().filterNot { it.name.equals(header.name, ignoreCase = true) } + header,
            )
            .build()
    }

    private fun ApolloResponse<*>.isAuthFailure(): Boolean {
        val e = exception
        return when {
            e is ApolloHttpException && e.statusCode in AUTH_HTTP_STATUS -> true
            e is ApolloWebSocketClosedException && e.code in AUTH_WS_CLOSE_CODES -> true
            e?.message.looksUnauthorized() -> true
            else -> errors?.any { it.message.looksUnauthorized() } == true
        }
    }

    companion object {
        private val log = logging()
        private val AUTH_HTTP_STATUS = setOf(401, 403)
        private val AUTH_WS_CLOSE_CODES = setOf(4401, 4403)
    }
}
