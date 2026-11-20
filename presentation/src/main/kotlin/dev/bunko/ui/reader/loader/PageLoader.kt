/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader.loader

import dev.bunko.ui.reader.model.ReaderPage
import kotlinx.coroutines.flow.StateFlow
import okio.BufferedSource

abstract class PageLoader {
    var isRecycled = false
        private set

    open fun recycle() {
        isRecycled = true
    }

    abstract fun getPages(): StateFlow<PagesState>

    abstract fun loadPage(page: ReaderPage)

    open fun preloadFirstPages(count: Int) {}

    open fun cancelPending() {}

    open fun retryPage(page: ReaderPage) {}

    open suspend fun <T> readPage(page: ReaderPage, block: (BufferedSource) -> T): T? = null
}

sealed class PagesState {
    data object Loading : PagesState()

    data class Success(
        val pages: List<ReaderPage>,
    ) : PagesState()

    data object Empty : PagesState()
}
