package dev.bunko.data.util

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import dev.bunko.domain.server.ServerReachability
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

internal class ApolloReachabilityInterceptor(
    private val reachability: ServerReachability,
) : ApolloInterceptor {
    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain,
    ): Flow<ApolloResponse<D>> =
        chain.proceed(request).onEach { response ->
            val exception = response.exception
            when {
                exception !is ApolloNetworkException -> reachability.reportReachable()
                !exception.isSlowResponse() -> reachability.reportUnreachable()
            }
        }

    private fun Throwable.isSlowResponse(): Boolean {
        val causes = generateSequence(this) { it.cause }
        return causes.none { it is ConnectTimeoutException } &&
            causes.any { it is HttpRequestTimeoutException || it is SocketTimeoutException }
    }
}
