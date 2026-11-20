/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.schedule

import com.diamondedge.logging.logging
import dev.bunko.core.di.AppScope
import dev.bunko.domain.library.service.LibraryRepository
import dev.bunko.domain.manga.model.Manga
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

data class ScheduledSeries(
    val manga: Manga,
    val categoryIds: List<Long>,
    val estimate: ReleaseEstimate,
)

@AppScope
@Inject
class GetReleaseSchedule(
    private val libraryRepository: LibraryRepository,
) {
    private val aniList by lazy {
        AniListStatuses(
            HttpClient(OkHttp) {
                install(HttpTimeout) {
                    connectTimeoutMillis = 10.seconds.inWholeMilliseconds
                    requestTimeoutMillis = 20.seconds.inWholeMilliseconds
                }
            },
            Json { ignoreUnknownKeys = true },
        )
    }

    suspend fun await(): Result<List<ScheduledSeries>> =
        try {
            val now = Clock.System.now().toEpochMilliseconds()
            val histories = libraryRepository.getReleaseHistory(since = now - WINDOW.inWholeMilliseconds).first()
            val statuses = aniListStatuses(histories)
            Result.success(histories.map { ScheduledSeries(it.manga, it.categoryIds, estimateRelease(it, statuses[it.aniListId], now)) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun aniListStatuses(histories: List<ReleaseHistory>): Map<Long, AniListStatus> {
        val ids = histories.mapNotNull { it.aniListId }
        if (ids.isEmpty()) return emptyMap()
        return try {
            aniList.lookup(ids)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "AniList status lookup failed, estimating from chapters only" }
            emptyMap()
        }
    }

    private companion object {
        val WINDOW = 365.days
        val log = logging()
    }
}
