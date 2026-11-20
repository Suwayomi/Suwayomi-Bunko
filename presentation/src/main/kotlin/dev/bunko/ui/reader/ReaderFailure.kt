/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import dev.bunko.domain.server.ConnectionError
import dev.bunko.domain.server.toConnectionError
import dev.bunko.i18n.MR
import dev.bunko.ui.settings.userMessage
import dev.icerock.moko.resources.StringResource

sealed interface ReaderFailure {
    data object OfflinePageMissing : ReaderFailure

    data object Decode : ReaderFailure

    data class Server(
        val error: ConnectionError,
    ) : ReaderFailure
}

fun Throwable.toReaderFailure(): ReaderFailure = ReaderFailure.Server(toConnectionError())

val ReaderFailure.message: StringResource
    get() = when (this) {
        ReaderFailure.OfflinePageMissing -> MR.strings.reader_error_page_missing
        ReaderFailure.Decode -> MR.strings.reader_error_decode
        is ReaderFailure.Server -> error.userMessage
    }
