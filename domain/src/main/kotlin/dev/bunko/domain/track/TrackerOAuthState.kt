/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.track

import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import io.ktor.http.parseQueryString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val TRACKER_OAUTH_REDIRECT = "bunko://tracker-oauth"

@Serializable
data class OAuthState(
    val redirectUrl: String,
    val trackerId: Int,
    val trackerName: String,
    val nonce: String,
)

private val stateJson = Json { ignoreUnknownKeys = true }

fun authUrlWithState(authUrl: String, state: OAuthState): String {
    val separator = if ('?' in authUrl) '&' else '?'
    return "$authUrl${separator}state=${stateJson.encodeToString(OAuthState.serializer(), state).encodeURLParameter()}"
}

fun parseOAuthState(uri: String): OAuthState? {
    if (!uri.startsWith(TRACKER_OAUTH_REDIRECT) || uri.getOrNull(TRACKER_OAUTH_REDIRECT.length) !in listOf(null, '?', '#', '/')) {
        return null
    }
    val url = runCatching { Url(uri) }.getOrNull() ?: return null
    val raw = url.parameters["state"]
        ?: runCatching { parseQueryString(url.fragment)["state"] }.getOrNull()
        ?: return null
    return runCatching { stateJson.decodeFromString(OAuthState.serializer(), raw) }.getOrNull()
        ?.takeIf { it.redirectUrl == TRACKER_OAUTH_REDIRECT }
}
