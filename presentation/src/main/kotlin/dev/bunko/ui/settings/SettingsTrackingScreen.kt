/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import dev.bunko.domain.track.model.Tracker
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.ChoicePreference
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.ui.base.prefs.SwitchPreference
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.manga.tracking.TrackerLogo
import dev.bunko.ui.manga.tracking.trackerIconUrl
import dev.bunko.ui.settings.tracking.CredentialsDialog
import dev.bunko.ui.settings.tracking.DiscordLink
import dev.bunko.ui.settings.tracking.DiscordTokenDialog
import dev.bunko.ui.settings.tracking.LogoutDialog
import dev.bunko.ui.settings.tracking.SettingsTrackingViewModel
import dev.bunko.ui.settings.tracking.TrackersState
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource

class SettingsTrackingScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { settingsTrackingViewModel() }
        SettingsTrackingScreenContent(vm)
    }
}

@Composable
private fun SettingsTrackingScreenContent(vm: SettingsTrackingViewModel) {
    val trackers by vm.trackers.collectAsState()
    val credentialsFor by vm.credentialsFor.collectAsState()
    val discordUsername by vm.discordUsername.collectAsState()
    val discordLink by vm.discordLink.collectAsState()
    val incognito by vm.incognito.collectAsState()
    val uriHandler = LocalUriHandler.current
    var logoutOf by remember { mutableStateOf<Tracker?>(null) }
    var tokenDialog by remember { mutableStateOf(false) }
    LaunchedEffect(discordUsername) {
        if (discordUsername.isNotEmpty()) tokenDialog = false
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.settings_tracking_screen))
        },
    ) {
        Box(Modifier.padding(it)) {
            val state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = WindowInsets.bottomNav.add(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Bottom,
                    ),
                ).asPaddingValues(),
            ) {
                item { PreferenceSectionHeader(stringResource(MR.strings.settings_tracking_trackers)) }
                item {
                    Text(
                        stringResource(MR.strings.settings_tracking_trackers_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                when (val current = trackers) {
                    TrackersState.Loading -> item {
                        LinearProgressIndicator(Modifier.padding(16.dp).fillMaxWidth())
                    }

                    is TrackersState.Failed -> item {
                        PreferenceRow(
                            title = stringResource(current.message),
                            subtitle = stringResource(MR.strings.action_retry),
                            onClick = vm::loadTrackers,
                        )
                    }

                    is TrackersState.Loaded -> items(current.trackers, key = { it.id }) { tracker ->
                        TrackerRow(
                            tracker = tracker,
                            serverUrl = vm.serverUrl,
                            onLogin = { vm.login(tracker)?.let { url -> runCatching { uriHandler.openUri(url) } } },
                            onLogout = { logoutOf = tracker },
                        )
                    }
                }

                item { PreferenceSectionHeader(stringResource(MR.strings.discord)) }
                item { PreferenceRow(
                        title = stringResource(MR.strings.discord_disclaimer_title),
                        subtitle = stringResource(MR.strings.discord_disclaimer),
                    ) }
                item {
                    PreferenceRow(
                        title = stringResource(MR.strings.discord_account),
                        subtitle = if (discordUsername.isEmpty()) {
                            stringResource(MR.strings.discord_not_linked)
                        } else {
                            stringResource(MR.strings.discord_linked_as, discordUsername)
                        },
                        onClick = { if (discordUsername.isEmpty()) tokenDialog = true },
                        action = if (discordUsername.isEmpty()) {
                            null
                        } else {
                            {
                                TextButton(onClick = vm::unlinkDiscord, Modifier.align(Alignment.Center)) {
                                    Text(stringResource(MR.strings.discord_unlink))
                                }
                            }
                        },
                    )
                }
                item {
                    SwitchPreference(
                        preference = vm.discordEnabled,
                        title = stringResource(MR.strings.discord_rpc),
                        subtitle = stringResource(
                            if (incognito) MR.strings.discord_rpc_incognito else MR.strings.discord_rpc_sub,
                        ),
                        enabled = discordUsername.isNotEmpty(),
                    )
                }
                item {
                    ChoicePreference(
                        preference = vm.discordStatus,
                        choices = vm.discordStatusChoices,
                        title = stringResource(MR.strings.discord_status),
                        enabled = discordUsername.isNotEmpty(),
                    )
                }
                item {
                    SwitchPreference(
                        preference = vm.discordShowChapter,
                        title = stringResource(MR.strings.discord_show_chapter),
                        enabled = discordUsername.isNotEmpty(),
                    )
                }
                item {
                    SwitchPreference(
                        preference = vm.discordShowTimestamp,
                        title = stringResource(MR.strings.discord_show_timestamp),
                        enabled = discordUsername.isNotEmpty(),
                    )
                }
                item {
                    SwitchPreference(
                        preference = vm.discordShowCover,
                        title = stringResource(MR.strings.discord_show_cover),
                        subtitle = stringResource(MR.strings.discord_show_cover_sub),
                        enabled = discordUsername.isNotEmpty(),
                    )
                }
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(state),
                Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .windowInsetsPadding(
                        WindowInsets.bottomNav.add(
                            WindowInsets.navigationBars.only(
                                WindowInsetsSides.Bottom,
                            ),
                        ),
                    ),
            )
        }
    }

    credentialsFor?.let { tracker ->
        CredentialsDialog(
            trackerName = tracker.name,
            onDismiss = vm::dismissCredentials,
            onConfirm = { username, password -> vm.loginCredentials(tracker, username, password) },
        )
    }
    logoutOf?.let { tracker ->
        LogoutDialog(
            trackerName = tracker.name,
            onDismiss = { logoutOf = null },
            onConfirm = {
                logoutOf = null
                vm.logout(tracker)
            },
        )
    }
    if (tokenDialog) {
        DiscordTokenDialog(
            checking = discordLink == DiscordLink.Checking,
            error = (discordLink as? DiscordLink.Failed)?.message?.let { stringResource(it) },
            onDismiss = {
                tokenDialog = false
                vm.resetDiscordLink()
            },
            onConfirm = vm::linkDiscord,
        )
    }
}

@Composable
private fun TrackerRow(
    tracker: Tracker,
    serverUrl: String,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    val loggedIn = tracker.isLoggedIn && !tracker.isTokenExpired
    PreferenceRow(
        title = tracker.name,
        subtitle = stringResource(
            when {
                loggedIn -> MR.strings.tracker_logged_in
                tracker.isLoggedIn -> MR.strings.tracker_token_expired
                else -> MR.strings.tracker_logged_out
            },
        ),
        onClick = if (loggedIn) onLogout else onLogin,
        action = {
            Box(Modifier.align(Alignment.Center)) {
                TrackerLogo(remember(serverUrl, tracker.icon) { trackerIconUrl(serverUrl, tracker.icon) }, tracker.name, 36.dp)
            }
        },
    )
}
