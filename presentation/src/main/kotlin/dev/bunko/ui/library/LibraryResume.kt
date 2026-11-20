/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library

import androidx.compose.runtime.Immutable
import dev.bunko.domain.chapter.interactor.GetChapters
import dev.bunko.domain.chapter.interactor.GetLastReadChapter
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.reader.ReaderProgressSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import me.tatarka.inject.annotations.Inject

@Immutable
data class ResumeEntry(
    val manga: Manga,
    val chapter: Chapter,
    val inProgress: Boolean,
)

internal fun resumeChapter(
    chapters: List<Chapter>,
    lastReadId: Long,
): Chapter? {
    val ordered = chapters.sortedBy { it.index }
    val last = ordered.firstOrNull { it.id == lastReadId } ?: return null
    return if (!last.read) last else ordered.firstOrNull { it.index > last.index && !it.read }
}

@Inject
class LibraryResume(
    private val getLastReadChapter: GetLastReadChapter,
    private val getChapters: GetChapters,
    private val progressSync: ReaderProgressSync,
) {
    val entry: Flow<ResumeEntry?> = combine(getLastReadChapter.asFlow(), progressSync.status) { last, _ -> last }
        .mapLatest { last ->
            if (last == null) return@mapLatest null
            val chapters = getChapters.await(last.manga.id)?.let { progressSync.withPending(it) }
                ?: return@mapLatest null
            resumeChapter(chapters, last.chapter.id)?.let { chapter ->
                ResumeEntry(last.manga, chapter, inProgress = chapter.lastPageRead > 0)
            }
        }
}
