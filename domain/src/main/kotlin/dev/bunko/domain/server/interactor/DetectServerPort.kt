/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.server.interactor

import dev.bunko.domain.server.HttpNoAuth
import dev.bunko.domain.server.hasExplicitPort
import dev.bunko.domain.server.model.parseServerUrl
import dev.bunko.domain.server.model.serverResource
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

@Inject
class DetectServerPort(
    private val client: HttpNoAuth,
) {
    suspend fun await(
        server: String,
        pathPrefix: String,
    ): Int? {
        val trimmed = server.trim()
        if (hasExplicitPort(if ("://" in trimmed) trimmed else "http://$trimmed")) return null
        val base = parseServerUrl(trimmed, 0, pathPrefix) ?: return null
        val candidates = if (base.protocol == URLProtocol.HTTPS) {
            listOf(HTTPS_PORT, SUWAYOMI_PORT)
        } else {
            listOf(SUWAYOMI_PORT, HTTP_PORT)
        }
        return coroutineScope {
            val probes = candidates.map { port -> port to async { answers(parseServerUrl(trimmed, port, pathPrefix)?.toString()) } }
            val found = probes.firstOrNull { (_, probe) -> probe.await() }?.first
            probes.forEach { (_, probe) -> probe.cancel() }
            found
        }
    }

    private suspend fun answers(base: String?): Boolean {
        base ?: return false
        return withTimeoutOrNull(PROBE_TIMEOUT) {
            try {
                val response = client.value.post(serverResource(base, "api/graphql")) {
                    setBody(TextContent(PROBE_QUERY, ContentType.Application.Json))
                }
                when {
                    response.status.value in AUTH_STATUS -> true
                    response.status.isSuccess() -> response.bodyAsText().let { "\"data\"" in it || "\"errors\"" in it }
                    else -> false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ResponseException) {
                e.response.status.value in AUTH_STATUS
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    private companion object {
        const val SUWAYOMI_PORT = 4567
        const val HTTPS_PORT = 443
        const val HTTP_PORT = 80
        val AUTH_STATUS = setOf(401, 403)
        val PROBE_TIMEOUT = 4.seconds
        const val PROBE_QUERY = """{"query":"{ aboutServer { name } }"}"""
    }
}
