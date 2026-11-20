package dev.bunko.data.util

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import dev.bunko.data.graphql.FetchChapterPagesMutation
import dev.bunko.data.graphql.FetchChaptersMutation
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import io.ktor.client.network.sockets.ConnectTimeoutException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

object ApolloRetryInterceptor : ApolloInterceptor {
    private const val MAX_RETRIES = 2
    private const val BASE_DELAY_MS = 500L
    private val RETRYABLE_HTTP_STATUS = setOf(502, 503, 504)

    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain,
    ): Flow<ApolloResponse<D>> {
        if (!request.operation.isReplayable()) return chain.proceed(request)
        return flow {
            var attempt = 0
            while (true) {
                val response = chain.proceed(request).first()
                if (attempt >= MAX_RETRIES || !response.isTransientFailure()) {
                    emit(response)
                    return@flow
                }
                delay(BASE_DELAY_MS shl attempt++)
            }
        }
    }

    private fun Operation<*>.isReplayable() =
        this is Query<*> || this is FetchChaptersMutation || this is FetchChapterPagesMutation

    private fun ApolloResponse<*>.isTransientFailure(): Boolean = when (val e = exception) {
        is ApolloNetworkException -> generateSequence<Throwable>(e) { it.cause }.none { it is ConnectTimeoutException }
        is ApolloHttpException -> e.statusCode in RETRYABLE_HTTP_STATUS
        else -> false
    }
}
