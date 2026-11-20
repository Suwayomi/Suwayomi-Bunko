/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import dev.bunko.ui.base.writeAtomic
import okio.FileSystem
import okio.Path

internal class PendingProgressStore(
    private val file: Path,
    private val fs: FileSystem,
) {
    fun load(): Map<Long, ChapterProgress> {
        if (!fs.exists(file)) return emptyMap()
        return fs.read(file) { readUtf8() }
            .lineSequence()
            .mapNotNull(ChapterProgress::decode)
            .associateBy { it.chapterId }
    }

    fun save(pending: Collection<ChapterProgress>) {
        if (pending.isEmpty()) {
            fs.delete(file, mustExist = false)
        } else {
            fs.writeAtomic(file) { writeUtf8(pending.joinToString("\n") { it.encode() }) }
        }
    }
}
