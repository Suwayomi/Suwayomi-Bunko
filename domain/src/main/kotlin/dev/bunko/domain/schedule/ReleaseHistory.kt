/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.schedule

import dev.bunko.domain.manga.model.Manga

data class ReleaseHistory(
    val manga: Manga,
    val uploadDates: List<Long>,
    val latestUpload: Long?,
    val aniListId: Long?,
    val categoryIds: List<Long>,
)
