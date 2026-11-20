/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.source.model

import androidx.compose.runtime.Stable
import dev.bunko.domain.manga.model.Manga

@Stable
data class MangaPage(
    val mangaList: List<Manga>,
    val hasNextPage: Boolean,
)
