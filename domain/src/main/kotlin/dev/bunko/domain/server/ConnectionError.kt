/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.server

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

sealed interface ConnectionError {
    val detail: String

    data class Unauthorized(
        override val detail: String,
    ) : ConnectionError

    data class HostNotFound(
        override val detail: String,
    ) : ConnectionError

    data class Unreachable(
        override val detail: String,
    ) : ConnectionError

    data class Timeout(
        override val detail: String,
    ) : ConnectionError

    data class Tls(
        override val detail: String,
    ) : ConnectionError

    data class NotSuwayomi(
        override val detail: String,
    ) : ConnectionError

    data class Other(
        override val detail: String,
    ) : ConnectionError
}

val ConnectionError.isNetwork: Boolean
    get() = this is ConnectionError.HostNotFound ||
        this is ConnectionError.Unreachable ||
        this is ConnectionError.Timeout ||
        this is ConnectionError.Tls

private val AUTH_STATUS = setOf(401, 403)

fun Throwable.toConnectionError(): ConnectionError {
    val chain = generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).toList()
    val detail = chain.lastOrNull { !it.message.isNullOrBlank() }?.message ?: toString()
    val status = chain.firstNotNullOfOrNull { (it as? ResponseException)?.response?.status?.value }
    return when {
        status in AUTH_STATUS || chain.any { it.message.looksUnauthorized() } -> ConnectionError.Unauthorized(detail)
        status == 404 -> ConnectionError.NotSuwayomi(detail)
        chain.any { it is UnknownHostException } -> ConnectionError.HostNotFound(detail)
        chain.any { it is SSLException } -> ConnectionError.Tls(detail)
        chain.any { it.isConnectFailure() } -> ConnectionError.Unreachable(detail)
        chain.any { it is SocketTimeoutException || it is HttpRequestTimeoutException } -> ConnectionError.Timeout(detail)
        else -> ConnectionError.Other(detail)
    }
}

private const val MAX_CAUSE_DEPTH = 16

private fun Throwable.isConnectFailure() =
    this is ConnectException ||
        this is NoRouteToHostException ||
        this is ConnectTimeoutException ||
        (this is SocketTimeoutException && message.orEmpty().contains("connect", ignoreCase = true))

private fun String?.looksUnauthorized() =
    this != null && (contains("unauthorized", ignoreCase = true) || contains("unauthenticated", ignoreCase = true))
