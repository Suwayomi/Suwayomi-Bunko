/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import dev.bunko.domain.chapter.interactor.GetChapterPages
import dev.bunko.domain.reader.service.ReaderPreferences
import dev.bunko.domain.server.Http
import dev.bunko.ui.base.image.BitmapDecoderFactory
import dev.bunko.ui.base.offline.OfflineChapterStore
import dev.bunko.ui.reader.loader.PagesState
import dev.bunko.ui.reader.loader.ServerPageLoader
import dev.bunko.ui.reader.model.ReaderChapter
import com.diamondedge.logging.logging
import com.seiko.imageloader.cache.disk.DiskCache
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

class ChapterLoader(
    private val readerPreferences: ReaderPreferences,
    private val http: Http,
    private val chapterCache: DiskCache,
    private val offlineStore: OfflineChapterStore,
    private val bitmapDecoderFactory: BitmapDecoderFactory,
    private val getChapterPages: GetChapterPages,
) {
    fun loadChapter(chapter: ReaderChapter): StateFlow<PagesState> {
        if (chapterIsReady(chapter)) {
            return (chapter.state as ReaderChapter.State.Loaded).pages
        } else {
            chapter.state = ReaderChapter.State.Loading
            log.debug { "Loading pages for ${chapter.chapter.name}" }

            val loader = ServerPageLoader(chapter, readerPreferences, http, chapterCache, offlineStore, bitmapDecoderFactory, getChapterPages)

            val pages = loader.getPages()

            pages
                .dropWhile { it is PagesState.Loading }
                .take(1)
                .filterIsInstance<PagesState.Empty>()
                .onEach {
                    chapter.state = ReaderChapter.State.Error(Exception("No pages found"))
                }
                .launchIn(chapter.scope)

            chapter.pageLoader = loader
            chapter.state = ReaderChapter.State.Loaded(pages)
            return pages
        }
    }

    private fun chapterIsReady(chapter: ReaderChapter): Boolean = chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null

    private companion object {
        private val log = logging()
    }
}
