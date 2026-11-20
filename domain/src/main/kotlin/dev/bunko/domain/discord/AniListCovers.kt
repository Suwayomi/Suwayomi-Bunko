/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

sealed interface CoverLookup {
    data class Found(val url: String) : CoverLookup

    data object NotFound : CoverLookup

    data class Failed(val cause: Throwable) : CoverLookup
}

internal class AniListCovers(
    private val client: HttpClient,
    private val json: Json,
) {
    suspend fun lookup(title: String): CoverLookup =
        try {
            val response = client.post(API) {
                contentType(ContentType.Application.Json)
                setBody(coverQuery(title))
            }
            when {
                response.status == HttpStatusCode.NotFound -> CoverLookup.NotFound
                !response.status.isSuccess() -> CoverLookup.Failed(IllegalStateException("HTTP ${response.status.value}"))
                else -> json.decodeFromString<Response>(response.bodyAsText()).coverUrl()
                    ?.let(CoverLookup::Found)
                    ?: CoverLookup.NotFound
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            CoverLookup.Failed(e)
        }

    @Serializable
    private data class Response(val data: Data? = null) {
        fun coverUrl(): String? = data?.media?.coverImage?.let { it.extraLarge ?: it.large }?.takeIf { it.startsWith("https://") }
    }

    @Serializable
    private data class Data(@SerialName("Media") val media: Media? = null)

    @Serializable
    private data class Media(val coverImage: CoverImage? = null)

    @Serializable
    private data class CoverImage(val extraLarge: String? = null, val large: String? = null)

    private companion object {
        const val API = "https://graphql.anilist.co"
    }
}

internal fun coverQuery(title: String): String =
    buildJsonObject {
        put("query", "query(\$s:String){Media(search:\$s,type:MANGA){coverImage{extraLarge large}}}")
        putJsonObject("variables") { put("s", title) }
    }.toString()
