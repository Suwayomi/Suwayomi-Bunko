/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.manga.interactor

import com.diamondedge.logging.logging
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.ServerListeners
import dev.bunko.domain.category.service.CategoryRepository
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.chapter.service.ChapterRepository
import dev.bunko.domain.library.service.LibraryRepository
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.manga.service.MangaRepository
import dev.bunko.domain.track.service.TrackRepository
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject

data class MigrationOptions(
    val chapters: Boolean = true,
    val categories: Boolean = true,
    val tracking: Boolean = true,
    val deleteDownloads: Boolean = false,
)

@Inject
class MigrateManga(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val categoryRepository: CategoryRepository,
    private val libraryRepository: LibraryRepository,
    private val trackRepository: TrackRepository,
    private val serverListeners: ServerListeners,
) {
    suspend fun await(
        from: Manga,
        to: Manga,
        options: MigrationOptions,
        replace: Boolean,
    ): Result<Unit> = try {
        require(from.id != to.id) { "can't migrate an entry onto itself" }
        migrate(from, to, options, replace)
        Result.success(Unit)
    } catch (e: Exception) {
        e.throwIfCancellation()
        log.warn(e) { "Failed to migrate ${from.id} to ${to.id}" }
        Result.failure(e)
    } finally {
        serverListeners.updateManga(from.id, to.id)
        serverListeners.updateLibrary()
    }

    private suspend fun migrate(
        from: Manga,
        to: Manga,
        options: MigrationOptions,
        replace: Boolean,
    ) {
        if (!to.initialized) runCatching { mangaRepository.refreshManga(to.id).first() }.onFailure { it.throwIfCancellation() }
        val oldChapters = if (options.chapters || options.deleteDownloads) chapterRepository.getChapters(from.id).first() else emptyList()
        val oldCategories = if (options.categories) categoryRepository.getMangaCategories(from.id).first() else emptyList()
        val oldRecords = if (options.tracking || replace) trackRepository.records(from.id) else emptyList()

        if (options.chapters) {
            val newChapters = chapterRepository.fetchChapters(to.id).first()
            copyChapters(oldChapters, newChapters)
        }

        if (!to.inLibrary) libraryRepository.addMangaToLibrary(to.id).first()
        oldCategories.filter { it.id != 0L }.forEach { category ->
            categoryRepository.addMangaToCategory(to.id, category.id).first()
            serverListeners.updateCategoryManga(category.id)
        }

        if (options.tracking && oldRecords.isNotEmpty()) {
            val bound = trackRepository.records(to.id).map { it.trackerId to it.remoteId }.toSet()
            oldRecords.filter { (it.trackerId to it.remoteId) !in bound }.forEach {
                trackRepository.bind(to.id, it.trackerId, it.remoteId)
            }
        }

        if (!replace) return
        oldRecords.forEach { record ->
            runCatching { trackRepository.unbind(record.id, deleteRemote = false) }
                .onFailure {
                    it.throwIfCancellation()
                    log.warn(it) { "Couldn't unbind tracker ${record.trackerId} from ${from.id}" }
                }
        }
        if (options.deleteDownloads) {
            val downloaded = oldChapters.filter { it.downloaded }.map { it.id }
            if (downloaded.isNotEmpty()) chapterRepository.deleteDownloadedChapters(downloaded).first()
        }
        if (from.inLibrary) libraryRepository.removeMangaFromLibrary(listOf(from.id)).first()
        oldCategories.filter { it.id != 0L }.forEach { serverListeners.updateCategoryManga(it.id) }
    }

    private suspend fun copyChapters(
        old: List<Chapter>,
        new: List<Chapter>,
    ) {
        val highestRead = old.filter { it.read && it.chapterNumber >= 0 }.maxOfOrNull { it.chapterNumber }
        val read = if (highestRead == null) {
            emptyList()
        } else {
            new.filter { !it.read && it.chapterNumber >= 0 && it.chapterNumber <= highestRead }.map { it.id }
        }
        val byNumber = new.filter { it.chapterNumber >= 0 }.groupBy { it.chapterNumber }
        val bookmarked = old.filter { it.bookmarked && it.chapterNumber >= 0 }
            .flatMap { byNumber[it.chapterNumber].orEmpty() }
            .filter { !it.bookmarked }
            .map { it.id }
            .distinct()
        val started = old.filter { !it.read && it.lastPageRead > 0 && it.chapterNumber >= 0 }
            .mapNotNull { chapter -> byNumber[chapter.chapterNumber]?.firstOrNull()?.takeIf { !it.read && it.id !in read }?.let { it to chapter.lastPageRead } }

        if (read.isNotEmpty()) chapterRepository.updateChapters(read, read = true).first()
        if (bookmarked.isNotEmpty()) chapterRepository.updateChapters(bookmarked, bookmarked = true).first()
        started.forEach { (chapter, page) -> chapterRepository.updateChapter(chapter.id, lastPageRead = page).first() }
    }

    private companion object {
        private val log = logging()
    }
}
