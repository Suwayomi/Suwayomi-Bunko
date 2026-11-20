/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base

import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal const val TMP_EXT = ".tmp"

internal fun FileSystem.writeAtomic(
    target: Path,
    write: BufferedSink.() -> Unit,
) {
    val tmp = (target.toString() + TMP_EXT).toPath()
    try {
        write(tmp) { write() }
        atomicMove(tmp, target)
    } catch (e: Throwable) {
        delete(tmp)
        throw e
    }
}
