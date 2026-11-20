/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.updates.model

import androidx.compose.runtime.Immutable
import dev.bunko.domain.chapter.model.Chapter
import dev.bunko.domain.manga.model.Manga

@Immutable
data class MangaAndChapter(
    val manga: Manga,
    val chapter: Chapter,
)
