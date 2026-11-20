package dev.bunko.data.library

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.GetReleaseScheduleQuery
import dev.bunko.data.graphql.LibraryUpdateStatusChangedSubscription
import dev.bunko.data.manga.MangaRepositoryImpl.Companion.toManga
import dev.bunko.domain.schedule.ReleaseHistory
import dev.bunko.data.graphql.SetMangaInLibraryMutation
import dev.bunko.data.graphql.SetMangasInLibraryMutation
import dev.bunko.data.graphql.fragment.UpdaterJobsInfoFragment
import dev.bunko.data.util.dataOrRejected
import dev.bunko.data.util.resilient
import dev.bunko.domain.base.WebsocketService
import dev.bunko.domain.library.model.UpdaterJobsInfo
import dev.bunko.domain.library.service.LibraryRepository
import dev.bunko.domain.library.service.LibraryUpdateService
import dev.bunko.domain.server.Http
import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class LibraryRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
) : LibraryRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun addMangaToLibrary(mangaId: Long): Flow<Unit> =
        apolloClient.mutation(SetMangaInLibraryMutation(mangaId.toInt(), true))
            .toFlow()
            .map { it.dataOrRejected }

    override fun removeMangaFromLibrary(mangaIds: List<Long>): Flow<Unit> =
        apolloClient.mutation(SetMangasInLibraryMutation(mangaIds.map { it.toInt() }, false))
            .toFlow()
            .map { it.dataOrRejected }

    override fun libraryUpdateSubscription(): Flow<UpdaterJobsInfo> =
        resilient(
            name = "Library update subscription",
            onReconnecting = { LibraryUpdateService.status.update { if (it == WebsocketService.Status.RUNNING) WebsocketService.Status.STARTING else it } },
        ) {
            apolloClient.subscription(LibraryUpdateStatusChangedSubscription())
                .toFlow()
                .map { it.dataAssertNoErrors.libraryUpdateStatusChanged.jobsInfo.updaterJobsInfoFragment.toUpdaterJobsInfo() }
        }

    override fun getReleaseHistory(since: Long): Flow<List<ReleaseHistory>> =
        apolloClient.query(GetReleaseScheduleQuery(since))
            .toFlow()
            .map { response ->
                val data = response.dataAssertNoErrors
                val uploads = data.chapters.nodes.groupBy({ it.mangaId.toLong() }, { it.uploadDate })
                data.mangas.nodes.map { node ->
                    ReleaseHistory(
                        manga = node.mangaFragment.toManga(),
                        uploadDates = uploads[node.mangaFragment.id.toLong()].orEmpty(),
                        latestUpload = node.latestUploadedChapter?.uploadDate?.takeIf { it > 0 },
                        aniListId = node.trackRecords.nodes.firstNotNullOfOrNull { aniListId(it.remoteId, it.remoteUrl) },
                        categoryIds = node.categories.nodes.map { it.id.toLong() }.ifEmpty { listOf(0L) },
                    )
                }
            }

    private fun aniListId(
        remoteId: Long,
        remoteUrl: String,
    ): Long? = remoteId.takeIf { remoteUrl.contains("anilist.co") }

    private fun UpdaterJobsInfoFragment.toUpdaterJobsInfo() =
        UpdaterJobsInfo(
            finishedJobs = finishedJobs,
            isRunning = isRunning,
            skippedCategoriesCount = skippedCategoriesCount,
            skippedMangasCount = skippedMangasCount,
            totalJobs = totalJobs,
        )
}
