/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.accent

import dev.bunko.ui.base.writeAtomic
import okio.FileSystem
import okio.Path

internal data class CachedAccent(
    val coverHash: Int,
    val argb: Int?,
)

internal fun encodeAccents(accents: Map<Long, CachedAccent>): String =
    accents.entries.joinToString("\n") { (id, accent) ->
        "$id ${accent.coverHash} ${accent.argb?.toUInt()?.toString(16) ?: "-"}"
    }

internal fun decodeAccents(text: String): Map<Long, CachedAccent> =
    text.lineSequence()
        .mapNotNull { line ->
            val parts = line.split(' ')
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val hash = parts[1].toIntOrNull() ?: return@mapNotNull null
            val argb = if (parts[2] == "-") null else parts[2].toUIntOrNull(16)?.toInt() ?: return@mapNotNull null
            id to CachedAccent(hash, argb)
        }
        .toMap()

internal class CoverAccentFile(
    private val file: Path,
    private val fs: FileSystem,
) {
    fun load(): Map<Long, CachedAccent> = if (fs.exists(file)) decodeAccents(fs.read(file) { readUtf8() }) else emptyMap()

    fun save(accents: Map<Long, CachedAccent>) = fs.writeAtomic(file) { writeUtf8(encodeAccents(accents)) }
}
