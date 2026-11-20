/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.chapter.model

import androidx.compose.runtime.Immutable
import dev.bunko.domain.library.model.FilterState
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class ChapterListOptions(
    val unread: FilterState = FilterState.IGNORED,
    val downloaded: FilterState = FilterState.IGNORED,
    val bookmarked: FilterState = FilterState.IGNORED,
    val excludedScanlators: Set<String> = emptySet(),
    val sort: ChapterSort = ChapterSort.SOURCE,
    val descending: Boolean = true,
    val display: ChapterDisplay = ChapterDisplay.NAME,
) {
    val isFiltered: Boolean
        get() = unread != FilterState.IGNORED ||
            downloaded != FilterState.IGNORED ||
            bookmarked != FilterState.IGNORED ||
            excludedScanlators.isNotEmpty()

    // scanlators are specific to one entry, so they never become part of a default
    fun asDefault(): ChapterListOptions = copy(excludedScanlators = emptySet())

    companion object {
        const val META_KEY = "@@bunkoChapterOptions"
    }
}

@Serializable
enum class ChapterSort {
    SOURCE,
    NUMBER,
    UPLOAD_DATE,
    ALPHABETICAL,
}

@Serializable
enum class ChapterDisplay {
    NAME,
    NUMBER,
}
