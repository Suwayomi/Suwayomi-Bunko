/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.server

import com.diamondedge.logging.logging
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

fun parseFallbackOrigin(
    text: String,
    primaryScheme: String,
    primaryPort: Int,
): HttpUrl? {
    val trimmed = text.trim().trimEnd('/').ifEmpty { return null }
    val typedScheme = "://" in trimmed
    val url = (if (typedScheme) trimmed else "$primaryScheme://$trimmed").toHttpUrlOrNull() ?: return null
    val inheritPort = !hasExplicitPort(trimmed) && (!typedScheme || url.scheme == primaryScheme)
    return url.origin(if (inheritPort && primaryPort in 1..65535) primaryPort else url.port)
}

internal fun hasExplicitPort(address: String): Boolean {
    val authority = address.substringAfter("://").substringBefore('/').substringAfterLast('@')
    return if (authority.startsWith('[')) authority.substringAfter(']').startsWith(':') else ':' in authority
}

internal fun HttpUrl.origin(port: Int = this.port): HttpUrl = HttpUrl.Builder().scheme(scheme).host(host).port(port).build()

internal fun HttpUrl.sameOrigin(other: HttpUrl) = scheme == other.scheme && host == other.host && port == other.port

internal fun HttpUrl.withOrigin(origin: HttpUrl): HttpUrl = newBuilder().scheme(origin.scheme).host(origin.host).port(origin.port).build()

internal class ServerFailover(
    private val primary: HttpUrl,
    private val fallback: HttpUrl,
) : Interceptor {
    @Volatile
    private var onFallbackSince: TimeMark? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.sameOrigin(primary)) return chain.proceed(request)
        val since = onFallbackSince
        val preferPrimary = since == null || since.elapsedNow() > PRIMARY_RECHECK
        return if (preferPrimary) {
            try {
                tryPrimary(chain, request).also { onFallbackSince = null }
            } catch (e: IOException) {
                if (!e.isUnreachable()) throw e
                log.info { "Primary server unreachable (${e.message}), switching to fallback" }
                val response = try {
                    chain.proceed(request.to(fallback))
                } catch (f: IOException) {
                    throw FailoverException(primary = e, fallback = f)
                }
                response.also { onFallbackSince = TimeSource.Monotonic.markNow() }
            }
        } else {
            try {
                chain.proceed(request.to(fallback))
            } catch (e: IOException) {
                if (!e.isUnreachable()) throw e
                val response = try {
                    tryPrimary(chain, request)
                } catch (p: IOException) {
                    throw FailoverException(primary = p, fallback = e)
                }
                response.also { onFallbackSince = null }
            }
        }
    }

    private fun tryPrimary(
        chain: Interceptor.Chain,
        request: Request,
    ): Response = chain.withConnectTimeout(PRIMARY_CONNECT_TIMEOUT_S, TimeUnit.SECONDS).proceed(request.to(primary))

    private fun Request.to(origin: HttpUrl) = newBuilder().url(url.withOrigin(origin)).build()

    private companion object {
        const val PRIMARY_CONNECT_TIMEOUT_S = 3
        val PRIMARY_RECHECK = 2.minutes
        val log = logging()
    }
}

internal class FailoverException(
    primary: IOException,
    fallback: IOException,
) : IOException(listOfNotNull(primary.message, fallback.message).joinToString("\n"), fallback)

private fun IOException.isUnreachable() =
    this is ConnectException ||
        this is NoRouteToHostException ||
        this is UnknownHostException ||
        (this is SocketTimeoutException && message.orEmpty().contains("connect", ignoreCase = true))
