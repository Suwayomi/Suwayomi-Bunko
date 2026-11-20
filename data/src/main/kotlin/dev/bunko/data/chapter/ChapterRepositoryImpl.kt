package dev.bunko.data.chapter

import dev.bunko.data.util.dataOrRejected
import dev.bunko.core.prefs.Preference
import dev.bunko.data.ApolloAppClient
import dev.bunko.data.graphql.DeleteDownloadedChapterMutation
import dev.bunko.data.graphql.DeleteDownloadedChaptersMutation
import dev.bunko.data.graphql.FetchChapterPagesMutation
import dev.bunko.data.graphql.FetchChaptersMutation
import dev.bunko.data.graphql.GetLastReadChapterQuery
import dev.bunko.data.graphql.GetReadHistoryQuery
import dev.bunko.domain.chapter.model.ReadHistoryPage
import dev.bunko.data.graphql.GetMangaChaptersQuery
import dev.bunko.data.graphql.UpdateChapterMetaMutation
import dev.bunko.data.graphql.UpdateChapterMutation
import dev.bunko.data.graphql.UpdateChaptersMutation
import dev.bunko.data.graphql.fragment.ChapterFragment
import dev.bunko.data.graphql.fragment.ChapterWithMangaFragment
import dev.bunko.data.graphql.type.UpdateChapterPatchInput
import dev.bunko.data.manga.MangaRepositoryImpl.Companion.toManga
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.service.ChapterRepository
import dev.bunko.domain.server.Http
import dev.bunko.domain.server.MAX_PAGE_BYTES
import dev.bunko.domain.server.model.serverResource
import dev.bunko.domain.server.readBytesCapped
import dev.bunko.domain.updates.model.MangaAndChapter
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareGet
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ChapterRepositoryImpl(
    private val apolloAppClient: ApolloAppClient,
    private val http: Http,
    private val serverUrl: Preference<Url>,
) : ChapterRepository {
    val apolloClient: ApolloClient
        get() = apolloAppClient.value

    override fun getChapters(mangaId: Long): Flow<List<Chapter>> =
        apolloClient.query(
            GetMangaChaptersQuery(mangaId.toInt()),
        )
            .toFlow()
            .map {
                val data = it.dataAssertNoErrors
                data.chapters.nodes.map { it.chapterFragment.toChapter() }
            }

    override fun getLastRead(): Flow<MangaAndChapter?> =
        apolloClient.query(GetLastReadChapterQuery())
            .toFlow()
            .map { response ->
                response.dataAssertNoErrors.chapters.nodes.firstOrNull()
                    ?.chapterWithMangaFragment?.toMangaAndChapter()
                    ?.takeIf { (it.chapter.lastReadAt ?: 0L) > 0L }
            }

    override fun getReadHistory(
        since: Long,
        first: Int,
        offset: Int,
    ): Flow<ReadHistoryPage> =
        apolloClient.query(GetReadHistoryQuery(since, first, offset))
            .toFlow()
            .map { response ->
                val chapters = response.dataAssertNoErrors.chapters
                ReadHistoryPage(
                    entries = chapters.nodes.map { it.chapterWithMangaFragment.toMangaAndChapter() },
                    hasNextPage = chapters.pageInfo.hasNextPage,
                )
            }

    override fun updateChapter(
        chapterId: Long,
        bookmarked: Boolean?,
        read: Boolean?,
        lastPageRead: Int?,
    ): Flow<Unit> =
        apolloClient.mutation(
            UpdateChapterMutation(
                chapterId.toInt(),
                UpdateChapterPatchInput(
                    isBookmarked = Optional.presentIfNotNull(bookmarked),
                    isRead = Optional.presentIfNotNull(read),
                    lastPageRead = Optional.presentIfNotNull(lastPageRead),
                ),
            ),
        )
            .toFlow()
            .map {
                it.dataOrRejected
            }

    override fun updateChapters(
        chapterIds: List<Long>,
        bookmarked: Boolean?,
        read: Boolean?,
        lastPageRead: Int?,
    ): Flow<Unit> =
        apolloClient.mutation(
            UpdateChaptersMutation(
                chapterIds.map { it.toInt() },
                UpdateChapterPatchInput(
                    isBookmarked = Optional.presentIfNotNull(bookmarked),
                    isRead = Optional.presentIfNotNull(read),
                    lastPageRead = Optional.presentIfNotNull(lastPageRead),
                ),
            ),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun deleteDownloadedChapter(chapterId: Long): Flow<Unit> =
        apolloClient.mutation(
            DeleteDownloadedChapterMutation(
                chapterId.toInt(),
            ),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun deleteDownloadedChapters(chapterIds: List<Long>): Flow<Unit> =
        apolloClient.mutation(
            DeleteDownloadedChaptersMutation(
                chapterIds.map { it.toInt() },
            ),
        )
            .toFlow()
            .map {
                it.dataAssertNoErrors
            }

    override fun updateChapterMeta(
        chapterId: Long,
        key: String,
        value: String,
    ): Flow<Unit> =
        apolloClient.mutation(
            UpdateChapterMetaMutation(
                chapterId.toInt(),
                key,
                value,
            ),
        )
            .toFlow()
            .map {
                it.dataOrRejected
            }

    override fun fetchChapters(mangaId: Long): Flow<List<Chapter>> =
        apolloClient.mutation(
            FetchChaptersMutation(
                mangaId.toInt(),
            ),
        )
            .toFlow()
            .map {
                val chapters = it.dataAssertNoErrors
                chapters.fetchChapters!!.chapters.map { it.chapterFragment.toChapter() }
            }

    override fun getPages(chapterId: Long): Flow<List<String>> =
        apolloClient.mutation(
            FetchChapterPagesMutation(
                chapterId.toInt(),
            ),
        )
            .toFlow()
            .map {
                val chapters = it.dataAssertNoErrors
                chapters.fetchChapterPages!!.pages
            }

    override fun getPage(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): Flow<ByteArray> {
        val realUrl = Url(serverResource(serverUrl.get().toString(), url))

        return flow { emit(http.value.prepareGet(realUrl, block).execute { it.readBytesCapped(MAX_PAGE_BYTES) }) }
    }

    companion object {
        internal fun ChapterFragment.toChapter(): Chapter =
            Chapter(
                id = id.toLong(),
                url = url,
                name = name,
                uploadDate = uploadDate,
                chapterNumber = chapterNumber.toFloat(),
                scanlator = scanlator,
                mangaId = mangaId.toLong(),
                read = isRead,
                bookmarked = isBookmarked,
                lastPageRead = lastPageRead,
                index = sourceOrder,
                fetchedAt = fetchedAt,
                realUrl = realUrl,
                pageCount = pageCount,
                lastReadAt = lastReadAt,
                downloaded = isDownloaded,
                meta = Chapter.ChapterMeta(
                    bunkoPageOffset = meta.find { it.key == "@@bunkoPageOffset" }?.value?.toIntOrNull() ?: 0,
                    historyHiddenAt = meta.find { it.key == Chapter.META_HISTORY_HIDDEN_AT }?.value?.toLongOrNull() ?: 0,
                ),
            )

        internal fun ChapterWithMangaFragment.toMangaAndChapter(): MangaAndChapter =
            MangaAndChapter(
                manga.mangaFragment.toManga(),
                chapterFragment.toChapter(),
            )
    }
}
