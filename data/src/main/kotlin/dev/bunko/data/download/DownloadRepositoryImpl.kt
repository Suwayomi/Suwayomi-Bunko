/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data.download

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.ClearDownloaderMutation
import dev.bunko.data.graphql.DequeueChapterDownloadMutation
import dev.bunko.data.graphql.DownloadStatusChangedSubscription
import dev.bunko.data.graphql.DownloadStatusQuery
import dev.bunko.data.graphql.EnqueueChapterDownloadMutation
import dev.bunko.data.graphql.EnqueueChapterDownloadsMutation
import dev.bunko.data.graphql.ReorderChapterDownloadMutation
import dev.bunko.data.graphql.StartDownloaderMutation
import dev.bunko.data.graphql.StopDownloaderMutation
import dev.bunko.data.graphql.fragment.DownloadFragment
import dev.bunko.domain.download.model.DownloadChapter
import dev.bunko.domain.download.model.DownloadManga
import dev.bunko.data.util.resilient
import dev.bunko.domain.download.model.DownloadQueueItem
import dev.bunko.domain.download.model.DownloadState
import dev.bunko.domain.download.model.DownloadStatus
import dev.bunko.domain.download.model.DownloadUpdate
import dev.bunko.domain.download.model.DownloadUpdateType
import dev.bunko.domain.download.model.DownloadUpdates
import dev.bunko.domain.download.model.DownloaderState
import dev.bunko.domain.download.service.DownloadRepository
import dev.bunko.domain.server.Http
import dev.bunko.domain.download.service.DownloadService
import dev.bunko.domain.base.WebsocketService
import kotlinx.coroutines.flow.update
import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dev.bunko.data.graphql.type.DownloadState as GraphQLDownloadState
import dev.bunko.data.graphql.type.DownloadUpdateType as GraphQLDownloadUpdateType
import dev.bunko.data.graphql.type.DownloaderState as GraphQLDownloaderState

class DownloadRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
) : DownloadRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun startDownloading(): Flow<Unit> =
        apolloClient.mutation(
            StartDownloaderMutation(),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun stopDownloading(): Flow<Unit> =
        apolloClient.mutation(
            StopDownloaderMutation(),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun clearDownloadQueue(): Flow<Unit> =
        apolloClient.mutation(
            ClearDownloaderMutation(),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun queueChapterDownload(chapterId: Long): Flow<Unit> =
        apolloClient.mutation(
            EnqueueChapterDownloadMutation(chapterId.toInt()),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun stopChapterDownload(chapterId: Long): Flow<Unit> =
        apolloClient.mutation(
            DequeueChapterDownloadMutation(chapterId.toInt()),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun reorderChapterDownload(
        chapterId: Long,
        to: Int,
    ): Flow<Unit> =
        apolloClient.mutation(
            ReorderChapterDownloadMutation(chapterId.toInt(), to),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun batchDownload(chapterIds: List<Long>): Flow<Unit> =
        apolloClient.mutation(
            EnqueueChapterDownloadsMutation(chapterIds.map { it.toInt() }),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun downloadSubscription(): Flow<DownloadUpdates> =
        resilient(
            name = "Download subscription",
            onReconnecting = { DownloadService.status.update { if (it == WebsocketService.Status.RUNNING) WebsocketService.Status.STARTING else it } },
        ) {
            apolloClient.subscription(
                DownloadStatusChangedSubscription(),
            )
                .toFlow()
                .map {
                    val data = it.dataAssertNoErrors.downloadStatusChanged

                    DownloadUpdates(
                        data.initial?.map { it.downloadFragment.toDownloadQueueItem() },
                        data.omittedUpdates,
                        data.state.toClient(),
                        data.updates.map {
                            DownloadUpdate(
                                when (it.type) {
                                    GraphQLDownloadUpdateType.QUEUED -> DownloadUpdateType.QUEUED
                                    GraphQLDownloadUpdateType.DEQUEUED -> DownloadUpdateType.DEQUEUED
                                    GraphQLDownloadUpdateType.PAUSED -> DownloadUpdateType.PAUSED
                                    GraphQLDownloadUpdateType.STOPPED -> DownloadUpdateType.STOPPED
                                    GraphQLDownloadUpdateType.PROGRESS -> DownloadUpdateType.PROGRESS
                                    GraphQLDownloadUpdateType.FINISHED -> DownloadUpdateType.FINISHED
                                    GraphQLDownloadUpdateType.ERROR -> DownloadUpdateType.ERROR
                                    GraphQLDownloadUpdateType.POSITION -> DownloadUpdateType.POSITION
                                    GraphQLDownloadUpdateType.UNKNOWN__ -> null
                                },
                                it.download.downloadFragment.toDownloadQueueItem(),
                            )
                        },
                    )
                }
        }

    override fun downloadStatus(): Flow<DownloadStatus> =
        apolloClient.query(
            DownloadStatusQuery(),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors.downloadStatus
                DownloadStatus(
                    data.state.toClient(),
                    data.queue.map { it.downloadFragment.toDownloadQueueItem() },
                )
            }

    companion object {
        fun DownloadFragment.toDownloadQueueItem(): DownloadQueueItem =
            DownloadQueueItem(
                position = this.position,
                progress = this.progress.toFloat(),
                state = when (this.state) {
                    GraphQLDownloadState.QUEUED -> DownloadState.QUEUED
                    GraphQLDownloadState.DOWNLOADING -> DownloadState.DOWNLOADING
                    GraphQLDownloadState.FINISHED -> DownloadState.FINISHED
                    GraphQLDownloadState.ERROR -> DownloadState.ERROR
                    GraphQLDownloadState.UNKNOWN__ -> DownloadState.ERROR
                },
                tries = this.tries,
                chapter = DownloadChapter(
                    chapter.id.toLong(),
                    chapter.name,
                    chapter.pageCount,
                ),
                manga = DownloadManga(
                    manga.id.toLong(),
                    manga.title,
                    manga.thumbnailUrl,
                    manga.thumbnailUrlLastFetched ?: 0,
                ),
            )

        fun GraphQLDownloaderState.toClient(): DownloaderState =
            when (this) {
                GraphQLDownloaderState.STARTED -> DownloaderState.STARTED
                GraphQLDownloaderState.STOPPED -> DownloaderState.STOPPED
                GraphQLDownloaderState.UNKNOWN__ -> DownloaderState.STOPPED
            }
    }
}
