/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.library.model

import androidx.compose.runtime.Immutable

@Immutable
data class LibraryStatistics(
    val inLibrary: Int,
    val completed: Int,
    val inGlobalUpdate: Int,
    val started: Int,
    val local: Int,
    val totalChapters: Int,
    val readChapters: Int,
    val downloadedChapters: Int,
    val trackedEntries: Int,
    val meanScore: Double?,
    val trackersUsed: Int,
)
