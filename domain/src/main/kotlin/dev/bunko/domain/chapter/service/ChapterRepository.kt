package dev.bunko.domain.chapter.service

import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.model.ReadHistoryPage
import dev.bunko.domain.updates.model.MangaAndChapter
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    fun getChapters(mangaId: Long): Flow<List<Chapter>>

    fun getLastRead(): Flow<MangaAndChapter?>

    fun getReadHistory(
        since: Long,
        first: Int,
        offset: Int,
    ): Flow<ReadHistoryPage>

    fun updateChapter(
        chapterId: Long,
        bookmarked: Boolean? = null,
        read: Boolean? = null,
        lastPageRead: Int? = null,
    ): Flow<Unit>

    fun updateChapters(
        chapterIds: List<Long>,
        bookmarked: Boolean? = null,
        read: Boolean? = null,
        lastPageRead: Int? = null,
    ): Flow<Unit>

    fun deleteDownloadedChapter(chapterId: Long): Flow<Unit>

    fun deleteDownloadedChapters(chapterIds: List<Long>): Flow<Unit>

    fun updateChapterMeta(
        chapterId: Long,
        key: String,
        value: String,
    ): Flow<Unit>

    fun fetchChapters(mangaId: Long): Flow<List<Chapter>>

    fun getPages(chapterId: Long): Flow<List<String>>

    fun getPage(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): Flow<ByteArray>
}
