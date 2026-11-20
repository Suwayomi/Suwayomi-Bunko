/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.updates.interactor

import androidx.compose.runtime.Immutable
import dev.bunko.domain.ServerListeners
import dev.bunko.domain.chapter.interactor.GetChapters
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.manga.interactor.GetManga
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.updates.model.MangaAndChapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject
import kotlin.time.Instant

@Inject
class UpdatesPager(
    private val getRecentUpdates: GetRecentUpdates,
    private val getManga: GetManga,
    private val getChapters: GetChapters,
    private val serverListeners: ServerListeners,
) : CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    private val updatesMutex = Mutex()

    private data class Fetched(
        val page: List<MangaAndChapter>,
        val replace: Boolean,
    )

    private val fetchedUpdates = MutableSharedFlow<Fetched>()
    private val foldedUpdates = fetchedUpdates.runningFold(emptyList<Updates>()) { shown, (newUpdates, replace) ->
        val updates = if (replace) emptyList() else shown
        updates.ifEmpty {
            val first = newUpdates.firstOrNull()?.chapter ?: return@runningFold updates
            listOf(
                Updates.Date(
                    Instant.fromEpochSeconds(first.fetchedAt)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date,
                ),
            )
        } + newUpdates.fold(emptyList()) { list, (manga, chapter) ->
            val date = (list.lastOrNull() as? Updates.Update)?.let {
                val lastUpdateDate = Instant.fromEpochSeconds(it.chapter.fetchedAt)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                val chapterDate = Instant.fromEpochSeconds(chapter.fetchedAt)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                chapterDate.takeUnless { it == lastUpdateDate }
            }

            if (date == null) {
                list + Updates.Update(manga, chapter)
            } else {
                list + Updates.Date(date) + Updates.Update(manga, chapter)
            }
        }
    }.stateIn(this, SharingStarted.Eagerly, emptyList())

    private val mangaIds = foldedUpdates.map { updates ->
        updates.filterIsInstance<Updates.Update>().map { it.manga.id }
    }.stateIn(this, SharingStarted.Eagerly, emptyList())
    private val chapterIds = foldedUpdates.map { updates ->
        updates.filterIsInstance<Updates.Update>().map { it.chapter.id }
    }.stateIn(this, SharingStarted.Eagerly, emptyList())

    private val changedChapters = MutableStateFlow(emptyMap<Long, Chapter>())

    private val changedManga =
        serverListeners.mangaListener.runningFold(emptyMap<Long, Manga>()) { manga, updatedMangaIds ->
            val shown = updatedMangaIds.filter { it in mangaIds.value }.distinct()
            if (shown.isEmpty()) return@runningFold manga
            coroutineScope {
                val chapters = shown.map { async { getChapters.await(it) } }
                val fresh = shown.map { async { getManga.await(it) } }.awaitAll().filterNotNull().associateBy { it.id }
                val shownChapters = chapterIds.value.toHashSet()
                val freshChapters = chapters.awaitAll().filterNotNull().flatten().filter { it.id in shownChapters }
                changedChapters.update { it + freshChapters.associateBy { chapter -> chapter.id } }
                manga + fresh
            }
        }.stateIn(this, SharingStarted.Eagerly, emptyMap())

    val updates = combine(
        foldedUpdates,
        changedManga,
        changedChapters,
    ) { updates, changedManga, changedChapters ->
        updates.mapNotNull {
            when (it) {
                is Updates.Date -> it

                is Updates.Update -> {
                    val manga = changedManga[it.manga.id] ?: it.manga
                    if (!manga.inLibrary) {
                        null
                    } else {
                        it.copy(manga = manga, chapter = changedChapters[it.chapter.id] ?: it.chapter)
                    }
                }
            }
        }.withoutEmptyDates()
    }.stateIn(this, SharingStarted.Eagerly, emptyList())

    private fun List<Updates>.withoutEmptyDates() =
        filterIndexed { i, item -> item !is Updates.Date || getOrNull(i + 1) is Updates.Update }

    private val currentPage = MutableStateFlow(0)
    private val hasNextPage = MutableStateFlow(true)

    init {
        launch {
            serverListeners.libraryUpdated.collect { reload() }
        }
    }

    @Immutable
    sealed class Updates {
        @Immutable
        data class Update(
            val manga: Manga,
            val chapter: Chapter,
        ) : Updates()

        @Immutable
        data class Date(
            val date: String,
        ) : Updates() {
            constructor(date: LocalDate) : this(date.toString())
        }
    }

    fun loadNextPage(
        onComplete: (() -> Unit)? = null,
        onError: suspend (Throwable) -> Unit,
    ) {
        launch {
            if (hasNextPage.value && updatesMutex.tryLock()) {
                currentPage.value++
                if (!getUpdates(currentPage.value, onError)) {
                    currentPage.value--
                }
                updatesMutex.unlock()
            }
            onComplete?.invoke()
        }
    }

    /** Reloads the first page in place, keeping what is shown until it arrives. */
    suspend fun refresh() = reload()

    private suspend fun reload() {
        updatesMutex.lock()
        try {
            val updates = getRecentUpdates.await(1, onError = {}) ?: return
            currentPage.value = 1
            hasNextPage.value = updates.hasNextPage
            changedChapters.value = emptyMap()
            fetchedUpdates.emit(Fetched(updates.page, replace = true))
        } finally {
            updatesMutex.unlock()
        }
    }

    private suspend fun getUpdates(
        page: Int,
        onError: suspend (Throwable) -> Unit,
    ): Boolean {
        val updates = getRecentUpdates.await(page, onError) ?: return false
        hasNextPage.value = updates.hasNextPage
        fetchedUpdates.emit(Fetched(updates.page, replace = false))
        return true
    }
}
