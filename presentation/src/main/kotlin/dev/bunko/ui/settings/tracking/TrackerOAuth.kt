/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings.tracking

import dev.bunko.core.di.AppScope
import dev.bunko.domain.track.interactor.Tracking
import dev.bunko.domain.track.OAuthState
import dev.bunko.domain.track.TRACKER_OAUTH_REDIRECT
import dev.bunko.domain.track.authUrlWithState
import dev.bunko.domain.track.model.Tracker
import dev.bunko.domain.track.parseOAuthState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.tatarka.inject.annotations.Inject
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

sealed interface OAuthResult {
    data class LoggedIn(val trackerName: String) : OAuthResult

    data class Failed(val trackerName: String, val cause: Throwable?) : OAuthResult

    data class Expired(val trackerName: String) : OAuthResult
}

@AppScope
@Inject
class TrackerOAuth(
    private val tracking: Tracking,
) {
    private data class Pending(val state: OAuthState, val expiresAt: Instant)

    private var pending: Pending? = null

    private val _results = MutableSharedFlow<OAuthResult>(replay = 1, extraBufferCapacity = 1)
    val results: SharedFlow<OAuthResult> = _results.asSharedFlow()

    fun consumed() = _results.resetReplayCache()

    fun begin(tracker: Tracker): String? {
        val authUrl = tracker.authUrl?.takeIf { it.isNotBlank() } ?: return null
        val state = OAuthState(TRACKER_OAUTH_REDIRECT, tracker.id, tracker.name, newNonce())
        pending = Pending(state, Clock.System.now() + LIFETIME)
        return authUrlWithState(authUrl, state)
    }

    suspend fun handleCallback(uri: String) {
        val state = parseOAuthState(uri) ?: return
        val current = pending
        if (current == null || Clock.System.now() >= current.expiresAt) {
            pending = null
            _results.emit(OAuthResult.Expired(state.trackerName))
            return
        }
        if (current.state != state) return
        pending = null
        val result = tracking.loginOAuth(state.trackerId, uri).fold(
            onSuccess = { if (it) OAuthResult.LoggedIn(state.trackerName) else OAuthResult.Failed(state.trackerName, null) },
            onFailure = { OAuthResult.Failed(state.trackerName, it) },
        )
        _results.emit(result)
    }

    private fun newNonce(): String =
        ByteArray(24).also(secureRandom::nextBytes).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private companion object {
        val LIFETIME = 10.minutes
        val secureRandom = SecureRandom()
    }
}
