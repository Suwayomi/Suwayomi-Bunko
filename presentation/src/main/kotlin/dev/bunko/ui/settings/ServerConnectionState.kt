/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import dev.bunko.domain.server.ConnectionError
import dev.bunko.i18n.MR
import dev.icerock.moko.resources.StringResource

sealed interface ConnectionStatus {
    data object Checking : ConnectionStatus

    data object InvalidAddress : ConnectionStatus

    data class Connected(
        val serverVersion: String,
    ) : ConnectionStatus

    data class SignInRequired(
        val error: ConnectionError,
    ) : ConnectionStatus

    data class Failed(
        val error: ConnectionError,
    ) : ConnectionStatus
}

sealed interface SignInState {
    data object Idle : SignInState

    data object InProgress : SignInState

    data class Failed(
        val error: ConnectionError,
    ) : SignInState
}

sealed interface ServerSettingsState {
    data object Loading : ServerSettingsState

    data object Unavailable : ServerSettingsState

    data class Loaded(
        val settings: ServerSettings,
    ) : ServerSettingsState
}

val ConnectionError.userMessage: StringResource
    get() = if (this is ConnectionError.Other) MR.strings.error_generic else message

val ConnectionError.message: StringResource
    get() = when (this) {
        is ConnectionError.Unauthorized -> MR.strings.conn_error_unauthorized
        is ConnectionError.HostNotFound -> MR.strings.conn_error_host_not_found
        is ConnectionError.Unreachable -> MR.strings.conn_error_unreachable
        is ConnectionError.Timeout -> MR.strings.conn_error_timeout
        is ConnectionError.Tls -> MR.strings.conn_error_tls
        is ConnectionError.NotSuwayomi -> MR.strings.conn_error_not_suwayomi
        is ConnectionError.Other -> MR.strings.conn_error_other
    }
