/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.bunko.domain.settings.interactor.GetSettings
import dev.bunko.domain.settings.interactor.SetSettings
import dev.bunko.domain.settings.model.AuthMode
import dev.bunko.domain.settings.model.SetSettingsInput
import dev.bunko.domain.settings.model.Settings
import dev.bunko.i18n.MR
import dev.bunko.ui.base.prefs.ChoicePreference
import dev.bunko.ui.base.prefs.EditTextPreference
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.ui.base.prefs.SwitchPreference
import dev.bunko.uicore.components.keyboardHandler
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogContentPadding
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.dialog.rememberDialogState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class ServerSettingMutableStateFlow<T>(
    parent: StateFlow<Settings>,
    getSetting: (Settings) -> T,
    private val setSetting: (T) -> Unit,
    scope: CoroutineScope,
    private val state: MutableStateFlow<T> = MutableStateFlow(getSetting(parent.value)),
) : MutableStateFlow<T> by state {
    init {
        parent
            .onEach { state.value = getSetting(it) }
            .launchIn(scope)
    }

    override var value: T
        get() = state.value
        set(value) {
            setSetting(value)
        }
}

@Stable
class ServerSettings(
    private val getSettings: GetSettings,
    private val setSettings: SetSettings,
    private val scope: CoroutineScope,
    initial: Settings,
    private val onError: (Throwable) -> Unit,
) {
    val settings = MutableStateFlow(initial)

    val autoDownloadNewChapters = getServerFlow(
        getSetting = { it.autoDownloadNewChapters },
        getInput = { SetSettingsInput(autoDownloadNewChapters = it) },
    )
    val autoDownloadNewChaptersLimit = getServerFlow(
        getSetting = { it.autoDownloadNewChaptersLimit.toString() },
        getInput = { SetSettingsInput(autoDownloadNewChaptersLimit = it.toIntOrNull()) },
    )
    val backupInterval = getServerFlow(
        getSetting = { it.backupInterval.toString() },
        getInput = { SetSettingsInput(backupInterval = it.toIntOrNull()) },
    )
    val backupPath = getServerFlow(
        getSetting = { it.backupPath },
        getInput = { SetSettingsInput(backupPath = it) },
    )
    val backupTTL = getServerFlow(
        getSetting = { it.backupTTL.toString() },
        getInput = { SetSettingsInput(backupTTL = it.toIntOrNull()) },
    )
    val backupTime = getServerFlow(
        getSetting = { it.backupTime },
        getInput = { SetSettingsInput(backupTime = it) },
    )

    val authMode = getServerFlow(
        getSetting = { it.authMode },
        getInput = { SetSettingsInput(authMode = it) },
    )
    val authPassword = getServerFlow(
        getSetting = { it.authPassword },
        getInput = { SetSettingsInput(authPassword = it) },
    )
    val authUsername = getServerFlow(
        getSetting = { it.authUsername },
        getInput = { SetSettingsInput(authUsername = it) },
    )
    val debugLogsEnabled = getServerFlow(
        getSetting = { it.debugLogsEnabled },
        getInput = { SetSettingsInput(debugLogsEnabled = it) },
    )
    val downloadAsCbz = getServerFlow(
        getSetting = { it.downloadAsCbz },
        getInput = { SetSettingsInput(downloadAsCbz = it) },
    )
    val downloadsPath = getServerFlow(
        getSetting = { it.downloadsPath },
        getInput = { SetSettingsInput(downloadsPath = it) },
    )
    val electronPath = getServerFlow(
        getSetting = { it.electronPath },
        getInput = { SetSettingsInput(electronPath = it) },
    )
    val excludeCompleted = getServerFlow(
        getSetting = { it.excludeCompleted },
        getInput = { SetSettingsInput(excludeCompleted = it) },
    )
    val excludeEntryWithUnreadChapters = getServerFlow(
        getSetting = { it.excludeEntryWithUnreadChapters },
        getInput = { SetSettingsInput(excludeEntryWithUnreadChapters = it) },
    )
    val excludeNotStarted = getServerFlow(
        getSetting = { it.excludeNotStarted },
        getInput = { SetSettingsInput(excludeNotStarted = it) },
    )
    val excludeUnreadChapters = getServerFlow(
        getSetting = { it.excludeUnreadChapters },
        getInput = { SetSettingsInput(excludeUnreadChapters = it) },
    )
    val extensionRepos = getServerFlow(
        getSetting = { it.extensionRepos },
        getInput = { SetSettingsInput(extensionRepos = it) },
    )
    val flareSolverrEnabled = getServerFlow(
        getSetting = { it.flareSolverrEnabled },
        getInput = { SetSettingsInput(flareSolverrEnabled = it) },
    )
    val flareSolverrSessionName = getServerFlow(
        getSetting = { it.flareSolverrSessionName },
        getInput = { SetSettingsInput(flareSolverrSessionName = it) },
    )
    val flareSolverrSessionTtl = getServerFlow(
        getSetting = { it.flareSolverrSessionTtl.toString() },
        getInput = { SetSettingsInput(flareSolverrSessionTtl = it.toIntOrNull()) },
    )
    val flareSolverrTimeout = getServerFlow(
        getSetting = { it.flareSolverrTimeout.toString() },
        getInput = { SetSettingsInput(flareSolverrTimeout = it.toIntOrNull()) },
    )
    val flareSolverrUrl = getServerFlow(
        getSetting = { it.flareSolverrUrl },
        getInput = { SetSettingsInput(flareSolverrUrl = it) },
    )
    val globalUpdateInterval = getServerFlow(
        getSetting = { it.globalUpdateInterval.toString() },
        getInput = { SetSettingsInput(globalUpdateInterval = it.toDoubleOrNull()?.takeIf { it !in 0.01..5.99 }) },
    )

    val initialOpenInBrowserEnabled = getServerFlow(
        getSetting = { it.initialOpenInBrowserEnabled },
        getInput = { SetSettingsInput(initialOpenInBrowserEnabled = it) },
    )
    val ip = getServerFlow(
        getSetting = { it.ip },
        getInput = { SetSettingsInput(ip = it) },
    )
    val localSourcePath = getServerFlow(
        getSetting = { it.localSourcePath },
        getInput = { SetSettingsInput(localSourcePath = it) },
    )
    val maxSourcesInParallel = getServerFlow(
        getSetting = { it.maxSourcesInParallel.toString() },
        getInput = { SetSettingsInput(maxSourcesInParallel = it.toIntOrNull()) },
    )
    val port = getServerFlow(
        getSetting = { it.port.toString() },
        getInput = { SetSettingsInput(port = it.toIntOrNull()) },
    )
    val socksProxyEnabled = getServerFlow(
        getSetting = { it.socksProxyEnabled },
        getInput = { SetSettingsInput(socksProxyEnabled = it) },
    )
    val socksProxyHost = getServerFlow(
        getSetting = { it.socksProxyHost },
        getInput = { SetSettingsInput(socksProxyHost = it) },
    )
    val socksProxyPassword = getServerFlow(
        getSetting = { it.socksProxyPassword },
        getInput = { SetSettingsInput(socksProxyPassword = it) },
    )
    val socksProxyPort = getServerFlow(
        getSetting = { it.socksProxyPort },
        getInput = { SetSettingsInput(socksProxyPort = it) },
    )
    val socksProxyUsername = getServerFlow(
        getSetting = { it.socksProxyUsername },
        getInput = { SetSettingsInput(socksProxyUsername = it) },
    )
    val socksProxyVersion = getServerFlow(
        getSetting = { it.socksProxyVersion },
        getInput = { SetSettingsInput(socksProxyVersion = it) },
    )
    val systemTrayEnabled = getServerFlow(
        getSetting = { it.systemTrayEnabled },
        getInput = { SetSettingsInput(systemTrayEnabled = it) },
    )
    val updateMangas = getServerFlow(
        getSetting = { it.updateMangas },
        getInput = { SetSettingsInput(updateMangas = it) },
    )
    val webUIChannel = getServerFlow(
        getSetting = { it.webUIChannel },
        getInput = { SetSettingsInput(webUIChannel = it) },
    )
    val webUIFlavor = getServerFlow(
        getSetting = { it.webUIFlavor },
        getInput = { SetSettingsInput(webUIFlavor = it) },
    )
    val webUIInterface = getServerFlow(
        getSetting = { it.webUIInterface },
        getInput = { SetSettingsInput(webUIInterface = it) },
    )
    val webUIUpdateCheckInterval = getServerFlow(
        getSetting = { it.webUIUpdateCheckInterval },
        getInput = { SetSettingsInput(webUIUpdateCheckInterval = it) },
    )

    private fun <T> getServerFlow(
        getSetting: (Settings) -> T,
        getInput: (T) -> SetSettingsInput,
    ): MutableStateFlow<T> =
        ServerSettingMutableStateFlow(
            parent = settings,
            getSetting = getSetting,
            setSetting = {
                scope.launch {
                    val input = getInput(it)
                    setSettings.await(input, onError = onError)
                    val response = getSettings.await(onError = onError)
                    if (response != null) {
                        settings.value = response
                    }
                }
            },
            scope = scope,
        )
}

fun LazyListScope.ServerSettingsItems(serverSettings: ServerSettings) {
    item {
        val ipValue by serverSettings.ip.collectAsState()
        EditTextPreference(
            preference = serverSettings.ip,
            title = stringResource(MR.strings.host_ip),
            subtitle = stringResource(MR.strings.host_ip_sub, ipValue),
        )
    }
    item {
        val portValue by serverSettings.port.collectAsState()
        EditTextPreference(
            preference = serverSettings.port,
            title = stringResource(MR.strings.host_port),
            subtitle = stringResource(MR.strings.host_port_sub, portValue),
        )
    }
    item {
        val dialog = rememberDialogState()
        PreferenceRow(
            stringResource(MR.strings.extension_repos),
            subtitle = stringResource(MR.strings.extension_repos_sub),
            onClick = dialog::show,
        )
        val repos by serverSettings.extensionRepos.collectAsState()
        ExtensionReposDialog(
            dialog,
            repos,
            onSetRepos = {
                serverSettings.extensionRepos.value = it
            },
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.socksProxyEnabled,
            title = stringResource(MR.strings.host_socks_enabled),
        )
    }
    item {
        val socksProxyEnabled by serverSettings.socksProxyEnabled.collectAsState()
        val proxyHost by serverSettings.socksProxyHost.collectAsState()
        EditTextPreference(
            preference = serverSettings.socksProxyHost,
            title = stringResource(MR.strings.host_socks_host),
            subtitle = stringResource(MR.strings.host_socks_host_sub, proxyHost),
            enabled = socksProxyEnabled,
        )
    }
    item {
        val socksProxyEnabled by serverSettings.socksProxyEnabled.collectAsState()
        val proxyPort by serverSettings.socksProxyPort.collectAsState()
        EditTextPreference(
            preference = serverSettings.socksProxyPort,
            title = stringResource(MR.strings.host_socks_port),
            subtitle = stringResource(MR.strings.host_socks_port_sub, proxyPort),
            enabled = socksProxyEnabled,
        )
    }
    item {
        val socksProxyEnabled by serverSettings.socksProxyEnabled.collectAsState()
        EditTextPreference(
            preference = serverSettings.socksProxyUsername,
            title = stringResource(MR.strings.host_socks_username),
            enabled = socksProxyEnabled,
        )
    }
    item {
        val socksProxyEnabled by serverSettings.socksProxyEnabled.collectAsState()
        EditTextPreference(
            preference = serverSettings.socksProxyPassword,
            title = stringResource(MR.strings.host_socks_password),
            visualTransformation = PasswordVisualTransformation(),
            enabled = socksProxyEnabled,
        )
    }
    item {
        val socksProxyEnabled by serverSettings.socksProxyEnabled.collectAsState()
        ChoicePreference(
            preference = serverSettings.socksProxyVersion,
            choices = mapOf(
                4 to "SOCKS4",
                5 to "SOCKS5",
            ).toImmutableMap(),
            title = stringResource(MR.strings.host_socks_version),
            enabled = socksProxyEnabled,
        )
    }
    item {
        EditTextPreference(
            preference = serverSettings.globalUpdateInterval,
            title = stringResource(MR.strings.global_update_interval),
            subtitle = stringResource(MR.strings.global_update_interval_sub),
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.updateMangas,
            title = stringResource(MR.strings.update_manga_info),
            subtitle = stringResource(MR.strings.update_manga_info_sub),
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.excludeCompleted,
            title = stringResource(MR.strings.exclude_completed),
            subtitle = stringResource(MR.strings.exclude_completed_sub),
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.excludeUnreadChapters,
            title = stringResource(MR.strings.exclude_unread),
            subtitle = stringResource(MR.strings.exclude_unread_sub),
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.excludeNotStarted,
            title = stringResource(MR.strings.exclude_not_started),
            subtitle = stringResource(MR.strings.exclude_not_started_sub),
        )
    }
    item {
        EditTextPreference(
            preference = serverSettings.maxSourcesInParallel,
            title = stringResource(MR.strings.max_sources_parallel),
            subtitle = stringResource(MR.strings.max_sources_parallel_sub),
        )
    }

    item {
        SwitchPreference(
            preference = serverSettings.downloadAsCbz,
            title = stringResource(MR.strings.host_download_as_cbz),
            subtitle = stringResource(MR.strings.host_download_as_cbz_sub),
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.autoDownloadNewChapters,
            title = stringResource(MR.strings.download_new_chapters),
        )
    }
    item {
        EditTextPreference(
            preference = serverSettings.autoDownloadNewChaptersLimit,
            title = stringResource(MR.strings.download_chapter_limit),
            subtitle = stringResource(MR.strings.download_chapter_limit_sub),
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.excludeEntryWithUnreadChapters,
            title = stringResource(MR.strings.ignore_unread_entries),
        )
    }

    item {
        SwitchPreference(
            preference = serverSettings.debugLogsEnabled,
            title = stringResource(MR.strings.host_debug_logging),
            subtitle = stringResource(MR.strings.host_debug_logging_sub),
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.systemTrayEnabled,
            title = stringResource(MR.strings.host_system_tray),
            subtitle = stringResource(MR.strings.host_system_tray_sub),
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.initialOpenInBrowserEnabled,
            title = stringResource(MR.strings.host_open_in_browser),
            subtitle = stringResource(MR.strings.host_open_in_browser_sub),
        )
    }

    item {
        EditTextPreference(
            preference = serverSettings.backupInterval,
            title = stringResource(MR.strings.backup_interval),
            subtitle = stringResource(MR.strings.backup_interval_sub),
        )
    }
    item {
        EditTextPreference(
            preference = serverSettings.backupTTL,
            title = stringResource(MR.strings.backup_ttl),
            subtitle = stringResource(MR.strings.backup_ttl_sub),
        )
    }
    item {
        val dialog = rememberDialogState()
        val backupTime by serverSettings.backupTime.collectAsState()
        PreferenceRow(
            title = stringResource(MR.strings.backup_time),
            subtitle = formatBackupTime(parseBackupTime(backupTime)) + " · " + stringResource(MR.strings.backup_time_sub),
            onClick = dialog::show,
        )
        BackupTimeDialog(
            dialog,
            backupTime,
            onSetTime = {
                serverSettings.backupTime.value = it
            },
        )
    }

    item {
        ChoicePreference(
            preference = serverSettings.authMode,
            title = stringResource(MR.strings.host_auth),
            subtitle = stringResource(MR.strings.host_auth_sub),
            choices = (AuthMode.entries - AuthMode.UNKNOWN__).associateWith {
                when (it) {
                    AuthMode.NONE -> stringResource(MR.strings.no_auth)
                    AuthMode.BASIC_AUTH -> stringResource(MR.strings.basic_auth)
                    AuthMode.SIMPLE_LOGIN -> stringResource(MR.strings.simple_auth)
                    AuthMode.UI_LOGIN -> stringResource(MR.strings.ui_login)
                    AuthMode.UNKNOWN__ -> ""
                }
            }.toImmutableMap(),
        )
    }

    item {
        val authModeValue by serverSettings.authMode.collectAsState()
        EditTextPreference(
            preference = serverSettings.authUsername,
            title = stringResource(MR.strings.host_auth_username),
            enabled = authModeValue != AuthMode.NONE,
        )
    }
    item {
        val authModeValue by serverSettings.authMode.collectAsState()
        EditTextPreference(
            preference = serverSettings.authPassword,
            title = stringResource(MR.strings.host_auth_password),
            visualTransformation = PasswordVisualTransformation(),
            enabled = authModeValue != AuthMode.NONE,
        )
    }
    item {
        SwitchPreference(
            preference = serverSettings.flareSolverrEnabled,
            title = "FlareSolverr enabled",
            subtitle = "Use a FlareSolverr instance to bypass CloudFlare. Manual setup required",
        )
    }
    item {
        val flareSolverrEnabled by serverSettings.flareSolverrEnabled.collectAsState()
        EditTextPreference(
            preference = serverSettings.flareSolverrUrl,
            title = stringResource(MR.strings.flaresolverr_url),
            enabled = flareSolverrEnabled,
        )
    }
    item {
        val flareSolverrEnabled by serverSettings.flareSolverrEnabled.collectAsState()
        EditTextPreference(
            preference = serverSettings.flareSolverrTimeout,
            title = stringResource(MR.strings.flaresolverr_timeout),
            enabled = flareSolverrEnabled,
        )
    }
    item {
        val flareSolverrEnabled by serverSettings.flareSolverrEnabled.collectAsState()
        EditTextPreference(
            preference = serverSettings.flareSolverrSessionName,
            title = stringResource(MR.strings.flaresolverr_session_name),
            enabled = flareSolverrEnabled,
        )
    }
    item {
        val flareSolverrEnabled by serverSettings.flareSolverrEnabled.collectAsState()
        EditTextPreference(
            preference = serverSettings.flareSolverrSessionTtl,
            title = stringResource(MR.strings.flaresolverr_session_ttl),
            enabled = flareSolverrEnabled,
        )
    }
}

private val repoRegex =
    (
        "https:\\/\\/(?>www\\.|raw\\.)?(github|githubusercontent)\\.com" +
            "\\/([^\\/]+)\\/([^\\/]+)(?>(?>\\/tree|\\/blob)?\\/([^\\/\\n]*))?(?>\\/([^\\/\\n]*\\.json)?)?"
    ).toRegex()

@Composable
fun ExtensionReposDialog(
    state: DialogState,
    extensionRepos: List<String>,
    onSetRepos: (List<String>) -> Unit,
) {
    val repos = remember(state.isShowing) {
        extensionRepos.toMutableStateList()
    }
    var newRepo by remember(state.isShowing) { mutableStateOf("") }
    AppDialog(
        state = state,
        title = stringResource(MR.strings.extension_repos),
        confirm = DialogButton(stringResource(MR.strings.action_ok)) { onSetRepos(repos.toList()) },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        val repoMatches by remember { derivedStateOf { newRepo.matches(repoRegex) } }
        val addRepo = {
            if (repoMatches) {
                repos.add(newRepo)
                newRepo = ""
            }
        }
        Row(DialogContentPadding.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newRepo,
                onValueChange = { newRepo = it },
                singleLine = true,
                isError = newRepo.isNotBlank() && !repoMatches,
                modifier = Modifier.weight(1f).keyboardHandler(singleLine = true, enterAction = { addRepo() }),
            )
            IconButton(onClick = addRepo, enabled = repoMatches) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(MR.strings.action_add))
            }
        }
        LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
            items(repos) { item ->
                Row(DialogContentPadding.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { repos.remove(item) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(MR.strings.action_delete))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupTimeDialog(
    state: DialogState,
    backupTime: String,
    onSetTime: (String) -> Unit,
) {
    val time = remember(state.isShowing, backupTime) { parseBackupTime(backupTime) }
    if (!state.isShowing) return
    val picker = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
    AppDialog(
        state = state,
        title = stringResource(MR.strings.backup_time),
        confirm = DialogButton(stringResource(MR.strings.action_ok)) {
            onSetTime(formatBackupTime(LocalTime(picker.hour, picker.minute)))
        },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        TimePicker(picker, modifier = DialogContentPadding.align(Alignment.CenterHorizontally))
    }
}

private val MIDNIGHT = LocalTime(0, 0)

internal fun parseBackupTime(value: String): LocalTime {
    val parts = value.trim().split(':')
    if (parts.size != 2) return MIDNIGHT
    val hour = parts[0].toIntOrNull() ?: return MIDNIGHT
    val minute = parts[1].toIntOrNull() ?: return MIDNIGHT
    if (hour !in 0..23 || minute !in 0..59) return MIDNIGHT
    return LocalTime(hour, minute)
}

internal fun formatBackupTime(time: LocalTime): String =
    time.hour.toString().padStart(2, '0') + ":" + time.minute.toString().padStart(2, '0')
