/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.offline

import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.ui.base.TMP_EXT
import dev.bunko.ui.base.writeAtomic
import okio.BufferedSink
import okio.FileSystem
import okio.Path

class OfflineChapterStore(
    private val root: Path,
    private val fs: FileSystem,
) {
    private fun chapterDir(
        mangaId: Long,
        chapterId: Long,
    ) = root / mangaId.toString() / chapterId.toString()

    private fun metaPath(
        mangaId: Long,
        chapterId: Long,
    ) = chapterDir(mangaId, chapterId) / META

    fun isDownloaded(
        mangaId: Long,
        chapterId: Long,
    ): Boolean = fs.exists(metaPath(mangaId, chapterId))

    fun readMeta(
        mangaId: Long,
        chapterId: Long,
    ): OfflineChapterMeta? =
        runCatching { fs.read(metaPath(mangaId, chapterId)) { readUtf8() } }
            .getOrNull()
            ?.let(OfflineChapterMeta::decode)

    fun pagePath(
        mangaId: Long,
        chapterId: Long,
        index: Int,
    ): Path? = (chapterDir(mangaId, chapterId) / "$index$PAGE_EXT").takeIf(fs::exists)

    fun hasPage(
        mangaId: Long,
        chapterId: Long,
        index: Int,
    ): Boolean = pagePath(mangaId, chapterId, index) != null

    fun chaptersOf(mangaId: Long): List<Chapter> =
        fs.listOrNull(root / mangaId.toString()).orEmpty()
            .mapNotNull { dir -> dir.name.toLongOrNull()?.let { readMeta(mangaId, it) } }
            .map { it.chapter }

    fun all(): List<OfflineChapterMeta> =
        fs.listOrNull(root).orEmpty().flatMap { mangaDir ->
            val mangaId = mangaDir.name.toLongOrNull() ?: return@flatMap emptyList()
            fs.listOrNull(mangaDir).orEmpty().mapNotNull { dir ->
                dir.name.toLongOrNull()?.let { readMeta(mangaId, it) }
            }
        }

    fun sizeBytes(): Long {
        if (!fs.exists(root)) return 0L
        return fs.listRecursively(root).sumOf { fs.metadataOrNull(it)?.size ?: 0L }
    }

    fun writePage(
        mangaId: Long,
        chapterId: Long,
        index: Int,
        bytes: ByteArray,
    ) {
        val dir = chapterDir(mangaId, chapterId)
        fs.createDirectories(dir)
        writeAtomic(dir / "$index$PAGE_EXT") { write(bytes) }
    }

    fun commit(meta: OfflineChapterMeta) {
        writeAtomic(metaPath(meta.chapter.mangaId, meta.chapter.id)) { writeUtf8(meta.encode()) }
    }

    fun sweepTemp(
        mangaId: Long,
        chapterId: Long,
    ) {
        fs.listOrNull(chapterDir(mangaId, chapterId)).orEmpty()
            .filter { it.name.endsWith(TMP_EXT) }
            .forEach { fs.delete(it) }
    }

    fun coverPath(mangaId: Long): Path? = (root / mangaId.toString() / COVER).takeIf(fs::exists)

    fun writeCover(
        mangaId: Long,
        bytes: ByteArray,
    ) {
        val dir = root / mangaId.toString()
        fs.createDirectories(dir)
        writeAtomic(dir / COVER) { write(bytes) }
    }

    fun delete(
        mangaId: Long,
        chapterId: Long,
    ) {
        fs.deleteRecursively(chapterDir(mangaId, chapterId), mustExist = false)
        val mangaDir = root / mangaId.toString()
        if (fs.listOrNull(mangaDir).orEmpty().none { it.name.toLongOrNull() != null }) {
            fs.deleteRecursively(mangaDir, mustExist = false)
        }
    }

    fun deleteSeriesIfEmpty(mangaId: Long) {
        if (chaptersOf(mangaId).isEmpty()) fs.deleteRecursively(root / mangaId.toString(), mustExist = false)
    }

    fun clear() = fs.deleteRecursively(root, mustExist = false)

    private fun writeAtomic(
        target: Path,
        write: BufferedSink.() -> Unit,
    ) = fs.writeAtomic(target, write)

    private companion object {
        const val META = "chapter.meta"
        const val PAGE_EXT = ".page"
        const val COVER = "cover"
    }
}

class OfflineChapterMeta(
    val mangaTitle: String,
    val pageCount: Int,
    val chapter: Chapter,
) {
    fun encode(): String =
        listOf(
            mangaTitle,
            pageCount,
            chapter.id,
            chapter.url,
            chapter.name,
            chapter.uploadDate,
            chapter.chapterNumber,
            chapter.scanlator.orEmpty(),
            chapter.mangaId,
            chapter.read,
            chapter.bookmarked,
            chapter.lastPageRead,
            chapter.index,
        ).joinToString("\n") { it.toString().replace('\n', ' ') }

    companion object {
        fun decode(text: String): OfflineChapterMeta? {
            val f = text.split('\n')
            if (f.size < FIELDS) return null
            return runCatching {
                OfflineChapterMeta(
                    mangaTitle = f[0],
                    pageCount = f[1].toInt(),
                    chapter = Chapter(
                        id = f[2].toLong(),
                        url = f[3],
                        name = f[4],
                        uploadDate = f[5].toLong(),
                        chapterNumber = f[6].toFloat(),
                        scanlator = f[7].ifEmpty { null },
                        mangaId = f[8].toLong(),
                        read = f[9].toBoolean(),
                        bookmarked = f[10].toBoolean(),
                        lastPageRead = f[11].toInt(),
                        index = f[12].toInt(),
                        fetchedAt = 0,
                        realUrl = null,
                        pageCount = f[1].toInt(),
                        lastReadAt = null,
                        downloaded = false,
                        meta = Chapter.ChapterMeta(),
                    ),
                )
            }.getOrNull()
        }

        private const val FIELDS = 13
    }
}
