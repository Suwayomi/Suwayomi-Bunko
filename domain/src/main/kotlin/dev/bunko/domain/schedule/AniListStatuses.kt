/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.schedule

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal class AniListStatuses(
    private val client: HttpClient,
    private val json: Json,
) {
    suspend fun lookup(ids: Collection<Long>): Map<Long, AniListStatus> =
        ids.distinct().chunked(PER_PAGE).flatMap { chunk -> page(chunk) }.toMap()

    private suspend fun page(ids: List<Long>): List<Pair<Long, AniListStatus>> {
        val response = client.post(API) {
            contentType(ContentType.Application.Json)
            setBody(statusQuery(ids))
        }
        check(response.status.isSuccess()) { "AniList HTTP ${response.status.value}" }
        return json.decodeFromString<Response>(response.bodyAsText()).data?.page?.media.orEmpty()
            .mapNotNull { media -> media.status?.let(::parseStatus)?.let { media.id to it } }
    }

    @Serializable
    private data class Response(val data: Data? = null)

    @Serializable
    private data class Data(@kotlinx.serialization.SerialName("Page") val page: Page? = null)

    @Serializable
    private data class Page(val media: List<Media> = emptyList())

    @Serializable
    private data class Media(val id: Long, val status: String? = null)

    private companion object {
        const val API = "https://graphql.anilist.co"
        const val PER_PAGE = 50
    }
}

internal fun parseStatus(status: String): AniListStatus? =
    when (status) {
        "RELEASING" -> AniListStatus.Releasing
        "FINISHED" -> AniListStatus.Finished
        "HIATUS" -> AniListStatus.Hiatus
        "CANCELLED" -> AniListStatus.Cancelled
        "NOT_YET_RELEASED" -> AniListStatus.NotYetReleased
        else -> null
    }

private fun statusQuery(ids: List<Long>): String =
    buildJsonObject {
        put("query", "query(\$ids:[Int]){Page(perPage:50){media(id_in:\$ids,type:MANGA){id status}}}")
        putJsonObject("variables") { putJsonArray("ids") { ids.forEach { add(it) } } }
    }.toString()
