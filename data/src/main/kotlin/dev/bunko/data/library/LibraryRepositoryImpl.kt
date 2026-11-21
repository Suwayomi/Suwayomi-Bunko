package dev.bunko.data.library

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.GetLibraryStatisticsQuery
import dev.bunko.data.graphql.GetReleaseScheduleQuery
import dev.bunko.data.graphql.type.IncludeOrExclude
import dev.bunko.data.graphql.type.MangaStatus as GqlMangaStatus
import dev.bunko.data.graphql.type.UpdateStrategy as GqlUpdateStrategy
import dev.bunko.domain.library.model.LibraryStatistics
import dev.bunko.domain.source.model.Source.Companion.LOCAL_SOURCE_ID
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

    override fun getStatistics(): Flow<LibraryStatistics> =
        apolloClient.query(GetLibraryStatisticsQuery())
            .toFlow()
            .map { response ->
                val data = response.dataAssertNoErrors
                val mangas = data.mangas.nodes
                val settings = data.settings
                val tracks = mangas.flatMap { it.trackRecords.nodes }
                val scores = tracks.mapNotNull { track ->
                    val scale = track.tracker.scores
                    val index = scale.indexOf(track.displayScore)
                    if (index <= 0 || scale.size < 2) null else index * 10.0 / (scale.size - 1)
                }
                LibraryStatistics(
                    inLibrary = mangas.size,
                    completed = mangas.count {
                        it.status == GqlMangaStatus.COMPLETED && it.chapters.totalCount > 0 && it.unreadCount == 0
                    },
                    inGlobalUpdate = globalUpdateCount(
                        mangas = mangas,
                        categories = data.categories.nodes,
                        excludeCompleted = settings.excludeCompleted,
                        excludeNotStarted = settings.excludeNotStarted,
                        excludeUnread = settings.excludeUnreadChapters || settings.excludeEntryWithUnreadChapters,
                    ),
                    started = mangas.count { it.isStarted() },
                    local = mangas.count { it.sourceId == LOCAL_SOURCE_ID },
                    totalChapters = mangas.sumOf { it.chapters.totalCount },
                    readChapters = mangas.sumOf { (it.chapters.totalCount - it.unreadCount).coerceAtLeast(0) },
                    downloadedChapters = mangas.sumOf { it.downloadCount },
                    trackedEntries = mangas.count { it.trackRecords.nodes.isNotEmpty() },
                    meanScore = scores.takeIf { it.isNotEmpty() }?.average(),
                    trackersUsed = tracks.map { it.trackerId }.distinct().size,
                )
            }

    private fun GetLibraryStatisticsQuery.Node.isStarted() =
        chapters.totalCount > unreadCount || (lastReadChapter?.lastReadAt ?: 0) > 0

    private fun globalUpdateCount(
        mangas: List<GetLibraryStatisticsQuery.Node>,
        categories: List<GetLibraryStatisticsQuery.Node3>,
        excludeCompleted: Boolean,
        excludeNotStarted: Boolean,
        excludeUnread: Boolean,
    ): Int {
        val byMode = categories.groupBy({ it.includeInUpdate }, { it.id })
        val excluded = byMode[IncludeOrExclude.EXCLUDE].orEmpty().toSet()
        val updated = byMode[IncludeOrExclude.INCLUDE].orEmpty()
            .ifEmpty { byMode[IncludeOrExclude.UNSET].orEmpty() }
            .toSet()
        return mangas.count { manga ->
            val ids = manga.categories.nodes.map { it.id }.ifEmpty { listOf(0) }
            ids.any { it in updated } &&
                ids.none { it in excluded } &&
                manga.updateStrategy == GqlUpdateStrategy.ALWAYS_UPDATE &&
                !(excludeCompleted && manga.status == GqlMangaStatus.COMPLETED) &&
                !(excludeNotStarted && !manga.isStarted()) &&
                !(excludeUnread && manga.unreadCount > 0)
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
