/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.io

import dev.bunko.core.lang.withIOContext
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Source
import okio.buffer
import okio.source
import okio.use
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

suspend fun Source.saveTo(path: Path) {
    withIOContext {
        use { source ->
            FileSystem.SYSTEM
                .sink(path)
                .buffer()
                .use { it.writeAll(source) }
        }
    }
}

suspend fun Source.copyTo(sink: BufferedSink) {
    withIOContext {
        use { source ->
            sink.use { it.writeAll(source) }
        }
    }
}

fun ByteArray.source(): BufferedSource = Buffer().write(this)

suspend fun ByteReadChannel.toSource(context: CoroutineContext = EmptyCoroutineContext): Source =
    toInputStream(context[Job]).source()

suspend fun Path.deleteTemp() =
    withContext(NonCancellable) {
        withIOContext { FileSystem.SYSTEM.delete(this@deleteTemp, mustExist = false) }
    }
