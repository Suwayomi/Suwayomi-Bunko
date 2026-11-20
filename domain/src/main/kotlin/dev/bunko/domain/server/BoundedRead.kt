/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.server

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.IOException
import kotlinx.io.readByteArray

const val MAX_PAGE_BYTES = 64L * 1024 * 1024

class ResponseTooLargeException(limit: Long) : IOException("response exceeds $limit bytes")

// a hostile server can stream an endless body; an IOException fails the download instead of OOM-killing the app
suspend fun HttpResponse.readBytesCapped(limit: Long): ByteArray {
    contentLength()?.let { if (it > limit) throw ResponseTooLargeException(limit) }
    return bodyAsChannel().readBytesCapped(limit)
}

suspend fun ByteReadChannel.readBytesCapped(limit: Long): ByteArray {
    val bytes = readRemaining(limit + 1).readByteArray()
    if (bytes.size > limit) throw ResponseTooLargeException(limit)
    return bytes
}
