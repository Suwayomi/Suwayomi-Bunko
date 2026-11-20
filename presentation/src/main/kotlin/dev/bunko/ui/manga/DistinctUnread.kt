/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga

import dev.bunko.domain.chapter.model.Chapter

fun distinctUnread(chapters: List<Chapter>): List<Chapter> {
    val readNumbers = chapters.filter { it.read }.mapNotNullTo(HashSet()) { it.knownNumber() }
    val preferredGroup = chapters
        .filter { it.read || it.lastPageRead > 0 }
        .maxWithOrNull(compareBy<Chapter>({ it.lastReadAt ?: 0L }, { it.index }))
        ?.scanlator
    return chapters
        .filter { !it.read && it.knownNumber() !in readNumbers }
        .sortedBy { it.index }
        .groupBy { it.knownNumber() ?: it.id }
        .values
        .map { group -> group.firstOrNull { it.scanlator == preferredGroup } ?: group.first() }
}

private fun Chapter.knownNumber(): Float? = chapterNumber.takeIf { it >= 0f }
