package dev.bunko.data.util

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpEngine

class NonClosingHttpEngine(private val delegate: HttpEngine) : HttpEngine {
    override suspend fun execute(request: HttpRequest): HttpResponse = delegate.execute(request)

    override fun close() = Unit
}
