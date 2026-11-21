package dev.bunko.data.manga

import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.GetMangaLibraryQuery
import dev.bunko.data.graphql.GetMangaQuery
import dev.bunko.data.graphql.GetThumbnailUrlQuery
import dev.bunko.data.graphql.RefreshMangaMutation
import dev.bunko.data.graphql.DeleteMangaMetaMutation
import dev.bunko.data.graphql.SetMangaMetaMutation
import dev.bunko.data.graphql.fragment.LibraryMangaFragment
import dev.bunko.data.graphql.fragment.MangaFragment
import dev.bunko.data.source.SourceRepositoryImpl.Companion.toSource
import dev.bunko.data.util.toFlowWithCache
import dev.bunko.domain.chapter.model.ChapterListOptions
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.manga.model.MangaMeta
import dev.bunko.domain.manga.model.MangaStatus
import dev.bunko.domain.manga.model.UpdateStrategy
import dev.bunko.domain.manga.service.MangaRepository
import dev.bunko.domain.server.Http
import dev.bunko.domain.server.HttpNoAuth
import dev.bunko.domain.server.model.isAbsoluteHttpUrl
import com.apollographql.apollo.ApolloClient
import com.diamondedge.logging.logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import dev.bunko.data.graphql.type.MangaStatus as GqlMangaStatus
import dev.bunko.data.graphql.type.UpdateStrategy as GqlUpdateStrategy

class MangaRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
    private val httpNoAuth: HttpNoAuth,
) : MangaRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun getManga(
        mangaId: Long,
        cacheFirst: Boolean,
    ): Flow<Manga> =
        apolloClient.query(
            GetMangaQuery(mangaId.toInt()),
        )
            .toFlowWithCache(cacheFirst)
            .map {
                val data = it.dataAssertNoErrors
                data.manga.mangaFragment.toManga()
            }

    override fun refreshManga(mangaId: Long): Flow<Manga> =
        apolloClient.mutation(
            RefreshMangaMutation(mangaId.toInt()),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.fetchManga!!.manga.mangaFragment.toManga()
            }

    override fun getMangaLibrary(mangaId: Long): Flow<Manga> =
        apolloClient.query(
            GetMangaLibraryQuery(mangaId.toInt()),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.manga.libraryMangaFragment.toManga()
            }

    override fun getMangaThumbnail(
        mangaId: Long,
        block: HttpRequestBuilder.() -> Unit,
    ): Flow<ByteReadChannel> =
        apolloClient.query(
            GetThumbnailUrlQuery(mangaId.toInt()),
        )
            .toFlow()
            .map {
                val url = it.dataAssertNoErrors.manga.thumbnailUrl!!
                // an absolute url can point at any host, so it never gets the server credentials
                val client = if (isAbsoluteHttpUrl(url)) httpNoAuth else http
                client.value.get(url).bodyAsChannel()
            }

    override fun updateMangaMeta(
        mangaId: Long,
        key: String,
        value: String,
    ): Flow<Unit> =
        apolloClient.mutation(
            SetMangaMetaMutation(mangaId.toInt(), key, value),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.setMangaMeta!!.clientMutationId
            }

    override fun deleteMangaMeta(
        mangaId: Long,
        key: String,
    ): Flow<Unit> =
        apolloClient.mutation(
            DeleteMangaMetaMutation(mangaId.toInt(), key),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.deleteMangaMeta!!.clientMutationId
            }

    companion object {
        private val log = logging()

        private val metaJson = Json { ignoreUnknownKeys = true }

        private fun <T> List<T>.toMangaMeta(entry: (T) -> Pair<String, String>): MangaMeta {
            val values = associate(entry)
            return MangaMeta(
                bunkoReaderMode = values["@@bunkoReaderMode"].orEmpty(),
                chapterOptions = values[ChapterListOptions.META_KEY]?.let(::decodeChapterOptions),
                orientation = values[MangaMeta.ORIENTATION_KEY],
            )
        }

        // a value written by a newer or broken client falls back to the default instead of failing the whole manga
        private fun decodeChapterOptions(value: String): ChapterListOptions? =
            runCatching { metaJson.decodeFromString(ChapterListOptions.serializer(), value) }
                .onFailure { log.warn(it) { "Ignoring unreadable chapter options" } }
                .getOrNull()

        internal fun MangaFragment.toManga(): Manga =
            Manga(
                id = id.toLong(),
                sourceId = sourceId,
                url = url,
                title = title,
                thumbnailUrl = thumbnailUrl,
                thumbnailUrlLastFetched = thumbnailUrlLastFetched ?: 0,
                initialized = initialized,
                artist = artist,
                author = author,
                description = description,
                genre = genre,
                status = when (status) {
                    GqlMangaStatus.ONGOING -> MangaStatus.ONGOING
                    GqlMangaStatus.COMPLETED -> MangaStatus.COMPLETED
                    GqlMangaStatus.LICENSED -> MangaStatus.LICENSED
                    GqlMangaStatus.PUBLISHING_FINISHED -> MangaStatus.PUBLISHING_FINISHED
                    GqlMangaStatus.CANCELLED -> MangaStatus.CANCELLED
                    GqlMangaStatus.ON_HIATUS -> MangaStatus.ON_HIATUS
                    GqlMangaStatus.UNKNOWN, GqlMangaStatus.UNKNOWN__ -> MangaStatus.UNKNOWN
                },
                inLibrary = inLibrary,
                source = null,
                updateStrategy = when (updateStrategy) {
                    GqlUpdateStrategy.ALWAYS_UPDATE -> UpdateStrategy.ALWAYS_UPDATE
                    GqlUpdateStrategy.ONLY_FETCH_ONCE -> UpdateStrategy.ONLY_FETCH_ONCE
                    GqlUpdateStrategy.UNKNOWN__ -> UpdateStrategy.ALWAYS_UPDATE
                },
                freshData = false,
                meta = meta.toMangaMeta { it.key to it.value },
                realUrl = realUrl,
                lastFetchedAt = lastFetchedAt,
                chaptersLastFetchedAt = chaptersLastFetchedAt,
                inLibraryAt = inLibraryAt,
                unreadCount = null,
                downloadCount = null,
                chapterCount = null,
                lastChapterReadTime = null,
                age = age,
                chaptersAge = null,
            )

        internal fun LibraryMangaFragment.toManga(): Manga =
            Manga(
                id = id.toLong(),
                sourceId = sourceId,
                url = url,
                title = title,
                thumbnailUrl = thumbnailUrl,
                thumbnailUrlLastFetched = thumbnailUrlLastFetched ?: 0,
                initialized = initialized,
                artist = artist,
                author = author,
                description = description,
                genre = genre,
                status = when (status) {
                    GqlMangaStatus.ONGOING -> MangaStatus.ONGOING
                    GqlMangaStatus.COMPLETED -> MangaStatus.COMPLETED
                    GqlMangaStatus.LICENSED -> MangaStatus.LICENSED
                    GqlMangaStatus.PUBLISHING_FINISHED -> MangaStatus.PUBLISHING_FINISHED
                    GqlMangaStatus.CANCELLED -> MangaStatus.CANCELLED
                    GqlMangaStatus.ON_HIATUS -> MangaStatus.ON_HIATUS
                    GqlMangaStatus.UNKNOWN, GqlMangaStatus.UNKNOWN__ -> MangaStatus.UNKNOWN
                },
                inLibrary = inLibrary,
                source = source?.sourceFragment?.toSource(),
                updateStrategy = when (updateStrategy) {
                    GqlUpdateStrategy.ALWAYS_UPDATE -> UpdateStrategy.ALWAYS_UPDATE
                    GqlUpdateStrategy.ONLY_FETCH_ONCE -> UpdateStrategy.ONLY_FETCH_ONCE
                    GqlUpdateStrategy.UNKNOWN__ -> UpdateStrategy.ALWAYS_UPDATE
                },
                freshData = false,
                meta = meta.toMangaMeta { it.key to it.value },
                realUrl = realUrl,
                lastFetchedAt = lastFetchedAt,
                chaptersLastFetchedAt = chaptersLastFetchedAt,
                inLibraryAt = inLibraryAt,
                unreadCount = unreadCount,
                downloadCount = downloadCount,
                chapterCount = chapters.totalCount,
                lastChapterReadTime = lastReadChapter?.lastReadAt ?: 0,
                age = age,
                chaptersAge = null,
                bookmarkCount = bookmarkCount,
                latestUploadAt = latestUploadedChapter?.uploadDate,
                latestFetchAt = latestFetchedChapter?.fetchedAt,
                lastReadAt = maxOf(latestReadChapter?.lastReadAt ?: 0, lastReadChapter?.lastReadAt ?: 0).takeIf { it > 0 },
            )
    }
}
