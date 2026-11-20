/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.updates

import dev.bunko.ui.base.userMessage
import dev.bunko.core.lang.launchDefault
import dev.bunko.domain.chapter.interactor.DeleteChapterDownload
import dev.bunko.domain.chapter.interactor.UpdateChapter
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.download.interactor.BatchChapterDownload
import dev.bunko.domain.download.interactor.QueueChapterDownload
import dev.bunko.domain.download.interactor.StopChapterDownload
import dev.bunko.domain.download.service.DownloadService
import dev.bunko.domain.updates.interactor.GetRecentUpdates
import dev.bunko.domain.updates.interactor.UpdateLibrary
import dev.bunko.domain.updates.interactor.UpdatesPager
import dev.bunko.ui.base.chapter.ChapterDownloadItem
import dev.bunko.ui.base.offline.OfflineChapters
import dev.bunko.ui.base.offline.OfflineState
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class UpdatesScreenViewModel(
    private val queueChapterDownload: QueueChapterDownload,
    private val stopChapterDownload: StopChapterDownload,
    private val deleteChapterDownload: DeleteChapterDownload,
    private val getRecentUpdates: GetRecentUpdates,
    private val updateChapter: UpdateChapter,
    private val batchChapterDownload: BatchChapterDownload,
    private val updateLibrary: UpdateLibrary,
    private val updatesPager: UpdatesPager,
    private val offlineChapters: OfflineChapters,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val offlineStates = offlineChapters.states
    val deviceSaving = offlineChapters.states
        .map { states -> states.values.count { it is OfflineState.Downloading } }
        .stateIn(scope, SharingStarted.Eagerly, 0)
    val downloadQueue = DownloadService.downloadQueue
        .map { it.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    fun deleteDeviceCopy(chapter: Chapter) = offlineChapters.delete(listOf(chapter))

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val updates = updatesPager.updates.map { updates ->
        updates.map {
            when (it) {
                is UpdatesPager.Updates.Date -> UpdatesUI.Header(it.date)
                is UpdatesPager.Updates.Update -> UpdatesUI.Item(ChapterDownloadItem(it.manga, it.chapter))
            }
        }.toImmutableList()
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    private val selectedIds = MutableStateFlow<ImmutableList<Long>>(persistentListOf())
    private var lastSelectedId: Long? = null
    val selectedItems = combine(updates, selectedIds) { updates, selectedItems ->
        updates.asSequence()
            .filterIsInstance<UpdatesUI.Item>()
            .filter { it.chapterDownloadItem.isSelected(selectedItems) }
            .map { it.chapterDownloadItem }
            .toImmutableList()
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    val inActionMode = selectedIds.map { it.isNotEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        updatesPager.loadNextPage(
            onComplete = {
                _isLoading.value = false
            },
            onError = {
                toast(it.userMessage.toPlatformString())
            },
        )
        updates
            .map { updates ->
                updates.filterIsInstance<UpdatesUI.Item>().mapNotNull {
                    it.chapterDownloadItem.manga?.id
                }.toSet()
            }
            .combine(DownloadService.downloadQueue) { mangaIds, queue ->
                mangaIds to queue
            }
            .buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .onEach { (mangaIds, queue) ->
                val chapters = queue.filter { it.manga.id in mangaIds }
                updates.value.filterIsInstance<UpdatesUI.Item>().forEach {
                    it.chapterDownloadItem.updateFrom(chapters)
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(scope)
    }

    fun loadNextPage() {
        updatesPager.loadNextPage(
            onError = {
                toast(it.userMessage.toPlatformString())
            },
        )
    }

    private fun setRead(
        chapterIds: List<Long>,
        read: Boolean,
    ) {
        scope.launch {
            val mangaIds = updates.value.filterIsInstance<UpdatesUI.Item>().filter { it.chapterDownloadItem.chapter.id in chapterIds }
                .mapNotNull { it.chapterDownloadItem.manga?.id }
            updateChapter.await(chapterIds, mangaIds, read = read, onError = { toast(it.userMessage.toPlatformString()) })
            selectedIds.value = persistentListOf()
        }
    }

    fun markRead(id: Long?) = setRead(listOfNotNull(id).ifEmpty { selectedIds.value }, true)

    fun markUnread(id: Long?) = setRead(listOfNotNull(id).ifEmpty { selectedIds.value }, false)

    private fun setBookmarked(
        chapterIds: List<Long>,
        bookmark: Boolean,
    ) {
        scope.launch {
            val mangaIds = updates.value.filterIsInstance<UpdatesUI.Item>().filter { it.chapterDownloadItem.chapter.id in chapterIds }
                .mapNotNull { it.chapterDownloadItem.manga?.id }
            updateChapter.await(chapterIds, mangaIds, bookmarked = bookmark, onError = { toast(it.userMessage.toPlatformString()) })
            selectedIds.value = persistentListOf()
        }
    }

    fun bookmarkChapter(id: Long?) = setBookmarked(listOfNotNull(id).ifEmpty { selectedIds.value }, true)

    fun unBookmarkChapter(id: Long?) = setBookmarked(listOfNotNull(id).ifEmpty { selectedIds.value }, false)

    fun downloadChapter(chapter: Chapter?) {
        scope.launch {
            if (chapter == null) {
                val selectedIds = selectedIds.value
                batchChapterDownload.await(selectedIds, onError = { toast(it.userMessage.toPlatformString()) })
                this@UpdatesScreenViewModel.selectedIds.value = persistentListOf()
                return@launch
            }
            queueChapterDownload.await(chapter.id, onError = { toast(it.userMessage.toPlatformString()) })
        }
    }

    fun deleteDownloadedChapter(chapter: Chapter?) {
        scope.launchDefault {
            val items = if (chapter == null) {
                selectedItems.value
            } else {
                updates.value
                    .filterIsInstance<UpdatesUI.Item>()
                    .map { it.chapterDownloadItem }
                    .filter { it.chapter.id == chapter.id }
            }
            if (items.isEmpty()) return@launchDefault
            items.forEach { it.setDownloaded(false) }
            if (chapter == null) selectedIds.value = persistentListOf()
            deleteChapterDownload.await(
                items.map { it.chapter.id },
                items.map { it.chapter.mangaId }.distinct(),
                onError = {
                    toast(it.userMessage.toPlatformString())
                    items.forEach { item -> item.setDownloaded(true) }
                },
            )
        }
    }

    fun stopDownloadingChapter(chapter: Chapter) {
        scope.launchDefault {
            updates.value
                .filterIsInstance<UpdatesUI.Item>()
                .find { (chapterDownloadItem) ->
                    chapterDownloadItem.chapter.mangaId == chapter.mangaId &&
                        chapterDownloadItem.chapter.index == chapter.index
                }
                ?.chapterDownloadItem
                ?.stopDownloading(stopChapterDownload)
        }
    }

    fun selectAll() {
        scope.launchDefault {
            selectedIds.value = updates.value.filterIsInstance<UpdatesUI.Item>()
                .map { it.chapterDownloadItem.chapter.id }
                .toImmutableList()
        }
    }

    fun invertSelection() {
        scope.launchDefault {
            selectedIds.value = updates.value.filterIsInstance<UpdatesUI.Item>()
                .map { it.chapterDownloadItem.chapter.id }
                .minus(selectedIds.value)
                .toImmutableList()
        }
    }

    fun selectChapter(id: Long) {
        scope.launchDefault {
            selectedIds.value = selectedIds.value.plus(id).distinct().toImmutableList()
            lastSelectedId = id
        }
    }

    /** A long press while selecting extends the selection from the last picked chapter. */
    fun longPressChapter(id: Long) {
        scope.launchDefault {
            val visible = updates.value.filterIsInstance<UpdatesUI.Item>().map { it.chapterDownloadItem.chapter.id }
            val from = lastSelectedId?.takeIf { it in selectedIds.value }?.let(visible::indexOf) ?: -1
            val to = visible.indexOf(id)
            selectedIds.value = if (from < 0 || to < 0) {
                selectedIds.value.plus(id)
            } else {
                selectedIds.value.plus(visible.subList(minOf(from, to), maxOf(from, to) + 1))
            }.distinct().toImmutableList()
            lastSelectedId = id
        }
    }

    fun unselectChapter(id: Long) {
        scope.launchDefault {
            selectedIds.value = selectedIds.value.minus(id).toImmutableList()
        }
    }

    fun clearSelection() {
        scope.launchDefault {
            selectedIds.value = persistentListOf()
        }
    }

    fun updateLibrary() {
        scope.launchDefault { updateLibrary.await(onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    /** Pulling down reloads the list and asks the server to look for new chapters. */
    fun refresh() {
        if (_isRefreshing.value) return
        scope.launchDefault {
            _isRefreshing.value = true
            updatesPager.refresh()
            updateLibrary.await(onError = { toast(it.userMessage.toPlatformString()) })
            _isRefreshing.value = false
        }
    }

    override fun onDispose() {
        super.onDispose()
        updatesPager.cancel()
    }

    private companion object {
        private val log = logging()
    }
}

sealed class UpdatesUI {
    data class Item(
        val chapterDownloadItem: ChapterDownloadItem,
    ) : UpdatesUI()

    data class Header(
        val date: String,
    ) : UpdatesUI()
}
