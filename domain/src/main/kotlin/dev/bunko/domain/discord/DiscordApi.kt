/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord

import dev.bunko.domain.discord.model.DiscordUser
import dev.bunko.domain.discord.model.ExternalAsset
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

sealed interface TokenCheck {
    data class Valid(val username: String) : TokenCheck

    data object Invalid : TokenCheck

    data class Failed(val cause: Throwable) : TokenCheck
}

internal class DiscordApi(
    private val client: HttpClient,
    private val json: Json,
) {
    suspend fun checkToken(token: String): TokenCheck =
        try {
            val response = client.get("$API/users/@me") { header(HttpHeaders.Authorization, token) }
            when {
                response.status == HttpStatusCode.Unauthorized -> TokenCheck.Invalid
                !response.status.isSuccess() -> TokenCheck.Failed(IllegalStateException("HTTP ${response.status.value}"))
                else -> {
                    val user = json.decodeFromString<DiscordUser>(response.bodyAsText())
                    TokenCheck.Valid(user.globalName ?: user.username)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            TokenCheck.Failed(e)
        }

    suspend fun setStatus(token: String, status: String): HttpStatusCode =
        client.patch("$API/users/@me/settings") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("status", JsonPrimitive(status)) }.toString())
        }.status

    suspend fun externalAsset(applicationId: String, token: String, imageUrl: String): String? {
        val response = client.post("$API/applications/$applicationId/external-assets") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("urls", JsonArray(listOf(JsonPrimitive(imageUrl)))) }.toString())
        }
        if (!response.status.isSuccess()) return null
        return json.decodeFromString<List<ExternalAsset>>(response.bodyAsText())
            .firstOrNull()?.externalAssetPath?.let { "mp:$it" }
    }

    private companion object {
        const val API = "https://discord.com/api/v10"
    }
}
