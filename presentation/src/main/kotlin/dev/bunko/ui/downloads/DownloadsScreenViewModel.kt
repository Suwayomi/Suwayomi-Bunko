/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.downloads

import dev.bunko.ui.base.userMessage
import dev.bunko.domain.base.ServerSubscriptions
import dev.bunko.domain.download.interactor.ClearDownloadQueue
import dev.bunko.domain.download.interactor.QueueChapterDownload
import dev.bunko.domain.download.interactor.ReorderChapterDownload
import dev.bunko.domain.download.interactor.StartDownloading
import dev.bunko.domain.download.interactor.StopChapterDownload
import dev.bunko.domain.download.interactor.StopDownloading
import dev.bunko.domain.download.model.DownloadChapter
import dev.bunko.domain.download.service.DownloadService
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class DownloadsScreenViewModel(
    private val serverSubscriptions: ServerSubscriptions,
    private val startDownloading: StartDownloading,
    private val stopDownloading: StopDownloading,
    private val clearDownloadQueue: ClearDownloadQueue,
    private val queueChapterDownload: QueueChapterDownload,
    private val stopChapterDownload: StopChapterDownload,
    private val reorderChapterDownload: ReorderChapterDownload,
    private val contextWrapper: ContextWrapper,
    @Assisted standalone: Boolean,
) : ViewModel(contextWrapper) {
    private val uiScope = if (standalone) {
        MainScope()
    } else {
        null
    }

    override val scope: CoroutineScope
        get() = uiScope ?: super.scope

    val serviceStatus = DownloadService.status.asStateFlow()
    val downloaderStatus = DownloadService.downloaderStatus.asStateFlow()
    val downloadQueue = DownloadService.downloadQueue.map { it.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    fun start() {
        scope.launch { startDownloading.await(onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun pause() {
        scope.launch { stopDownloading.await(onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun clear() {
        scope.launch { clearDownloadQueue.await(onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun stopDownload(chapter: DownloadChapter) {
        scope.launch { stopChapterDownload.await(chapter.id, onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun moveUp(chapter: DownloadChapter) {
        scope.launch {
            val index =
                downloadQueue.value.indexOfFirst { it.chapter.id == chapter.id }
            if (index == -1 || index <= 0) return@launch
            reorderChapterDownload.await(chapter.id, index - 1, onError = { toast(it.userMessage.toPlatformString()) })
        }
    }

    fun moveDown(chapter: DownloadChapter) {
        scope.launch {
            val index =
                downloadQueue.value.indexOfFirst { it.chapter.id == chapter.id }
            if (index == -1 || index >= downloadQueue.value.lastIndex) return@launch
            reorderChapterDownload.await(chapter.id, index + 1, onError = { toast(it.userMessage.toPlatformString()) })
        }
    }

    fun moveToTop(chapter: DownloadChapter) {
        scope.launch {
            reorderChapterDownload.await(chapter.id, 0, onError = { toast(it.userMessage.toPlatformString()) })
        }
    }

    fun moveToBottom(chapter: DownloadChapter) {
        scope.launch {
            reorderChapterDownload.await(
                chapter.id,
                downloadQueue.value.lastIndex,
                onError = { toast(it.userMessage.toPlatformString()) },
            )
        }
    }

    fun restartDownloader() = serverSubscriptions.restartDownloads()

    override fun onDispose() {
        super.onDispose()
        uiScope?.cancel()
    }

    private companion object {
        private val log = logging()
    }
}
