/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings.tracking

import dev.bunko.domain.discord.DiscordPresence
import dev.bunko.domain.discord.TokenCheck
import dev.bunko.domain.discord.model.DiscordStatus
import dev.bunko.domain.discord.service.DiscordPreferences
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.track.interactor.Tracking
import dev.bunko.domain.track.model.Tracker
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.i18n.MR
import dev.bunko.ui.base.userMessage
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

sealed interface TrackersState {
    data object Loading : TrackersState

    data class Failed(val message: StringResource) : TrackersState

    data class Loaded(val trackers: ImmutableList<Tracker>) : TrackersState
}

sealed interface DiscordLink {
    data object Idle : DiscordLink

    data object Checking : DiscordLink

    data class Failed(val message: StringResource) : DiscordLink
}

@Inject
class SettingsTrackingViewModel(
    private val tracking: Tracking,
    private val trackerOAuth: TrackerOAuth,
    private val discordPresence: DiscordPresence,
    private val discordPreferences: DiscordPreferences,
    serverPreferences: ServerPreferences,
    uiPreferences: UiPreferences,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val serverUrl: String = serverPreferences.serverUrl().get().toString()

    private val _trackers = MutableStateFlow<TrackersState>(TrackersState.Loading)
    val trackers = _trackers.asStateFlow()

    private val _credentialsFor = MutableStateFlow<Tracker?>(null)
    val credentialsFor = _credentialsFor.asStateFlow()

    val discordEnabled = discordPreferences.enabled().asStateFlow()
    val discordStatus = discordPreferences.status().asStateFlow()
    val discordShowChapter = discordPreferences.showChapter().asStateFlow()
    val discordShowCover = discordPreferences.showCover().asStateFlow()
    val discordShowTimestamp = discordPreferences.showTimestamp().asStateFlow()
    val discordUsername = discordPreferences.username().asStateFlow()
    val incognito = uiPreferences.incognito().asStateFlow()

    private val _discordLink = MutableStateFlow<DiscordLink>(DiscordLink.Idle)
    val discordLink = _discordLink.asStateFlow()

    val discordStatusChoices = DiscordStatus.entries.associateWith { it.res.toPlatformString() }.toImmutableMap()

    init {
        loadTrackers()
        trackerOAuth.results
            .onEach { result ->
                when (result) {
                    is OAuthResult.LoggedIn -> toast(MR.strings.tracker_login_success.toPlatformString(result.trackerName))
                    is OAuthResult.Expired -> toast(MR.strings.tracker_login_expired.toPlatformString(result.trackerName))
                    is OAuthResult.Failed -> toast(
                        result.cause?.userMessage?.toPlatformString()
                            ?: MR.strings.tracker_login_failed.toPlatformString(result.trackerName),
                    )
                }
                trackerOAuth.consumed()
                loadTrackers()
            }
            .launchIn(scope)
    }

    fun loadTrackers() {
        scope.launch {
            if (_trackers.value !is TrackersState.Loaded) _trackers.value = TrackersState.Loading
            _trackers.value = tracking.trackers().fold(
                onSuccess = { TrackersState.Loaded(it.toImmutableList()) },
                onFailure = { TrackersState.Failed(it.userMessage) },
            )
        }
    }

    fun login(tracker: Tracker): String? {
        val url = trackerOAuth.begin(tracker)
        if (url == null) _credentialsFor.value = tracker
        return url
    }

    fun dismissCredentials() {
        _credentialsFor.value = null
    }

    fun loginCredentials(tracker: Tracker, username: String, password: String) {
        _credentialsFor.value = null
        scope.launch {
            tracking.loginCredentials(tracker.id, username.trim(), password)
                .onSuccess { loggedIn ->
                    toast(
                        if (loggedIn) {
                            MR.strings.tracker_login_success.toPlatformString(tracker.name)
                        } else {
                            MR.strings.tracker_login_failed.toPlatformString(tracker.name)
                        },
                    )
                }
                .onFailure { toast(it.userMessage.toPlatformString()) }
            loadTrackers()
        }
    }

    fun logout(tracker: Tracker) {
        scope.launch {
            tracking.logout(tracker.id).onFailure { toast(it.userMessage.toPlatformString()) }
            loadTrackers()
        }
    }

    fun linkDiscord(token: String) {
        if (token.isBlank()) return
        _discordLink.value = DiscordLink.Checking
        scope.launch {
            when (val check = discordPresence.checkToken(token)) {
                is TokenCheck.Valid -> {
                    discordPreferences.token().set(token.trim())
                    discordPreferences.username().set(check.username)
                    _discordLink.value = DiscordLink.Idle
                }

                TokenCheck.Invalid -> _discordLink.value = DiscordLink.Failed(MR.strings.discord_token_invalid)

                is TokenCheck.Failed -> _discordLink.value = DiscordLink.Failed(check.cause.userMessage)
            }
        }
    }

    fun resetDiscordLink() {
        _discordLink.value = DiscordLink.Idle
    }

    fun unlinkDiscord() {
        discordPreferences.unlink()
    }
}
