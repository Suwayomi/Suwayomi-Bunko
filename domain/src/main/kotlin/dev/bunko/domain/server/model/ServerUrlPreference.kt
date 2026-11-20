/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.server.model

import dev.bunko.core.prefs.Preference
import dev.bunko.core.prefs.getAsFlow
import dev.bunko.domain.server.hasExplicitPort
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn

// always joins with a single '/', so a path like "@evil.host/x" can never become the authority
fun serverResource(
    base: String,
    path: String,
): String = base.trimEnd('/') + "/" + path.trimStart('/')

fun isAbsoluteHttpUrl(url: String): Boolean = url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)

fun parseServerUrl(
    server: String,
    port: Int,
    pathPrefix: String,
): Url? {
    val trimmed = server.trim().trimEnd('/').ifEmpty { return null }
    val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
    val builder = runCatching { URLBuilder(withScheme) }.getOrNull() ?: return null
    if (builder.protocol != URLProtocol.HTTP && builder.protocol != URLProtocol.HTTPS) return null
    if (builder.host.isBlank()) return null
    if (!hasExplicitPort(withScheme)) builder.port = port
    pathPrefix.ifBlank { null }?.let { builder.path(it) }
    return builder.build()
}

class ServerUrlPreference(
    private val key: String,
    private val server: Preference<String>,
    private val port: Preference<Int>,
    private val pathPrefix: Preference<String>,
) : Preference<Url> {
    override fun key(): String = key

    override fun get(): Url = parseServerUrl(server.get(), port.get(), pathPrefix.get()) ?: defaultValue()

    override fun set(value: Url) {
        server.set(value.protocol.name + "://" + value.host)
        port.set(value.port)
        pathPrefix.set(value.encodedPath)
    }

    override fun isSet(): Boolean = server.isSet() || port.isSet() || pathPrefix.isSet()

    override fun delete() {
        server.delete()
        port.delete()
        pathPrefix.delete()
    }

    override fun defaultValue(): Url =
        URLBuilder(server.defaultValue()).apply {
            port = this@ServerUrlPreference.port.defaultValue()
        }.build()

    override fun changes(): Flow<Url> =
        combine(server.getAsFlow(), port.getAsFlow(), pathPrefix.getAsFlow()) { server, port, pathPrefix ->
            parseServerUrl(server, port, pathPrefix) ?: defaultValue()
        }.drop(1)

    override fun stateIn(scope: CoroutineScope): StateFlow<Url> = changes().stateIn(scope, SharingStarted.Eagerly, get())
}
