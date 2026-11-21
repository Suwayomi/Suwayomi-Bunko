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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.core.lang.launchIO
import dev.bunko.domain.server.interactor.DetectServerPort
import dev.bunko.domain.server.ConnectionError
import dev.bunko.domain.server.credentialOrigins
import dev.bunko.domain.server.isNetwork
import dev.bunko.domain.server.movedFrom
import dev.bunko.domain.server.model.Auth
import dev.bunko.domain.server.model.Proxy
import dev.bunko.domain.server.model.parseServerUrl
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.server.toConnectionError
import dev.bunko.domain.settings.interactor.AboutServer
import dev.bunko.domain.settings.interactor.GetSettings
import dev.bunko.domain.settings.interactor.SetSettings
import dev.bunko.domain.user.interactor.UserLoginSimple
import dev.bunko.domain.user.interactor.UserLoginUI
import dev.bunko.domain.user.interactor.UserLogout
import dev.bunko.domain.user.service.UserPreferences
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.ChoicePreference
import dev.bunko.ui.base.prefs.EditTextPreference
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.prefs.asStateIn
import dev.bunko.uicore.prefs.asStringStateIn
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.Length
import dev.bunko.uicore.vm.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.milliseconds

class SettingsServerScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { settingsServerViewModel() }
        SettingsServerScreenContent(vm)
    }
}

@OptIn(FlowPreview::class)
@Inject
class SettingsServerViewModel(
    private val aboutServer: AboutServer,
    private val detectServerPort: DetectServerPort,
    private val getSettings: GetSettings,
    private val setSettings: SetSettings,
    serverPreferences: ServerPreferences,
    userPreferences: UserPreferences,
    private val userLoginSimple: UserLoginSimple,
    private val userLoginUi: UserLoginUI,
    private val userLogout: UserLogout,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    private val serverUrl = serverPreferences.server().asStateIn(scope)
    private val serverPort = serverPreferences.port().asStringStateIn(scope)
    private val serverPathPrefix = serverPreferences.pathPrefix().asStateIn(scope)
    private val serverFallback = serverPreferences.fallbackServer().asStateIn(scope)

    val serverAddress = combine(serverUrl, serverPort, serverPathPrefix, serverFallback) { url, port, prefix, fallback ->
        ServerAddress(url, port.toPortField(), prefix, fallback)
    }
        .stateIn(scope, SharingStarted.Eagerly, currentAddress())

    val proxy = serverPreferences.proxy().asStateIn(scope)
    val httpHost = serverPreferences.proxyHttpHost().asStateIn(scope)
    val httpPort = serverPreferences.proxyHttpPort().asStringStateIn(scope)
    val socksHost = serverPreferences.proxySocksHost().asStateIn(scope)
    val socksPort = serverPreferences.proxySocksPort().asStringStateIn(scope)

    val auth = serverPreferences.auth().asStateIn(scope)
    val authUsername = serverPreferences.authUsername().asStateIn(scope)
    val authPassword = serverPreferences.authPassword().asStateIn(scope)

    val hasSession = combine(auth, userPreferences.uiRefreshToken().asStateIn(scope), userPreferences.simpleSession().asStateIn(scope)) {
        auth,
        refreshToken,
        session,
        ->
        when (auth) {
            Auth.UI -> refreshToken.isNotBlank()
            Auth.SIMPLE -> session.isNotBlank()
            else -> false
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _connection = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Checking)
    val connection = _connection.asStateFlow()

    private val _signIn = MutableStateFlow<SignInState>(SignInState.Idle)
    val signIn = _signIn.asStateFlow()

    private val _serverSettings = MutableStateFlow<ServerSettingsState>(ServerSettingsState.Loading)
    val serverSettings = _serverSettings.asStateFlow()

    private val recheck = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var checkJob: Job? = null
    private var settingsScope: CoroutineScope? = null

    @Composable
    fun getProxyChoices(): ImmutableMap<Proxy, String> =
        persistentMapOf(
            Proxy.NO_PROXY to stringResource(MR.strings.no_proxy),
            Proxy.HTTP_PROXY to stringResource(MR.strings.http_proxy),
            Proxy.SOCKS_PROXY to stringResource(MR.strings.socks_proxy),
        )

    @Composable
    fun getAuthChoices(): ImmutableMap<Auth, String> =
        persistentMapOf(
            Auth.NONE to stringResource(MR.strings.no_auth),
            Auth.BASIC to stringResource(MR.strings.basic_auth),
            Auth.SIMPLE to stringResource(MR.strings.simple_auth),
            Auth.UI to stringResource(MR.strings.ui_login),
        )

    fun saveAddress(address: ServerAddress) {
        val typedPort = address.portNumber ?: return
        val url = address.url.trim()
        val pathPrefix = address.pathPrefix.trim()
        val fallback = address.fallback.trim()
        scope.launch {
            val port = if (typedPort == 0) {
                _connection.value = ConnectionStatus.Checking
                detectServerPort.await(url, pathPrefix) ?: 0
            } else {
                typedPort
            }
            val before = credentialOrigins(serverUrl.value, serverPort.value.toIntOrNull() ?: 0, serverFallback.value)
            val moved = credentialOrigins(url, port, fallback).movedFrom(before)
            // clear before saving, so no request ever pairs the new host with the old credentials
            if (moved) signOutForAddressChange()
            serverUrl.value = url
            serverPort.value = port.toString()
            serverPathPrefix.value = pathPrefix
            serverFallback.value = fallback
            requestCheck()
        }
    }

    private fun missingCredentials() =
        auth.value in SIGN_IN_AUTH && !hasSession.value && authPassword.value.isEmpty()

    private suspend fun signOutForAddressChange() {
        val wasSignedIn = hasSession.value || authPassword.value.isNotEmpty()
        userLogout.await()
        authPassword.value = ""
        _signIn.value = SignInState.Idle
        if (wasSignedIn) toast(MR.strings.signed_out_server_changed.toPlatformString(), Length.LONG)
    }

    fun onSignIn(
        username: String,
        password: String,
    ) {
        if (_signIn.value is SignInState.InProgress) return
        authUsername.value = username
        authPassword.value = password
        when (auth.value) {
            Auth.SIMPLE, Auth.UI -> login(auth.value, username, password)
            Auth.NONE, Auth.BASIC, Auth.DIGEST -> requestCheck()
        }
    }

    fun onSignOut() {
        scope.launch {
            userLogout.await()
            _signIn.value = SignInState.Idle
            checkConnection()
        }
    }

    fun checkConnection() {
        checkJob?.cancel()
        if (parseServerUrl(serverUrl.value, serverPort.value.toIntOrNull() ?: 0, serverPathPrefix.value) == null) {
            _connection.value = ConnectionStatus.InvalidAddress
            replaceServerSettings(ServerSettingsState.Unavailable)
            return
        }
        _connection.value = ConnectionStatus.Checking
        replaceServerSettings(ServerSettingsState.Loading)
        checkJob = scope.launchIO {
            var error: Throwable? = null
            val about = aboutServer.await(onError = { error = it })
            val settings = about?.let { getSettings.await(onError = { error = it }) }
            ensureActive()
            if (about != null && settings != null) {
                replaceServerSettings {
                    ServerSettingsState.Loaded(ServerSettings(getSettings, setSettings, it, settings, ::onSettingSaveFailed))
                }
                _connection.value = ConnectionStatus.Connected(about.version)
            } else {
                val cause = error?.toConnectionError() ?: ConnectionError.Other("Empty response")
                _connection.value = if (cause is ConnectionError.Unauthorized || (!cause.isNetwork && missingCredentials())) {
                    ConnectionStatus.SignInRequired(cause)
                } else {
                    ConnectionStatus.Failed(cause)
                }
                replaceServerSettings(ServerSettingsState.Unavailable)
            }
        }
    }

    private fun requestCheck() {
        _connection.value = ConnectionStatus.Checking
        recheck.tryEmit(Unit)
    }

    private fun login(
        mode: Auth,
        username: String,
        password: String,
    ) {
        _signIn.value = SignInState.InProgress
        scope.launch {
            var error: Throwable? = null
            val result = if (mode == Auth.SIMPLE) {
                userLoginSimple.await(username, password, onError = { error = it })
            } else {
                userLoginUi.await(username, password, onError = { error = it })
            }
            if (result != null) {
                _signIn.value = SignInState.Idle
                checkConnection()
            } else {
                val cause = error?.toConnectionError() ?: ConnectionError.Other("Empty response")
                val shown = if (cause.isNetwork ||
                    cause is ConnectionError.NotSuwayomi
                ) {
                    cause
                } else {
                    ConnectionError.Unauthorized(cause.detail)
                }
                _signIn.value = SignInState.Failed(shown)
            }
        }
    }

    private fun onSettingSaveFailed(error: Throwable) {
        val reason = error.toConnectionError().message.toPlatformString()
        toast(MR.strings.server_setting_save_failed.toPlatformString(reason), Length.LONG)
    }

    private fun replaceServerSettings(state: ServerSettingsState) = replaceServerSettings { state }

    private fun replaceServerSettings(create: (CoroutineScope) -> ServerSettingsState) {
        settingsScope?.cancel()
        val child = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
        settingsScope = child
        _serverSettings.value = create(child)
    }

    private fun currentAddress() = ServerAddress(serverUrl.value, serverPort.value.toPortField(), serverPathPrefix.value, serverFallback.value)

    private fun String.toPortField() = takeUnless { it == "0" }.orEmpty()

    init {
        val connectionPrefs = listOf<Flow<Any>>(
            serverUrl,
            serverPort,
            serverPathPrefix,
            serverFallback,
            proxy,
            httpHost,
            httpPort,
            socksHost,
            socksPort,
            auth,
            authUsername,
            authPassword,
        )
        merge(combine(connectionPrefs) { it.toList() }.drop(1).map { }, recheck)
            .onEach { _connection.value = ConnectionStatus.Checking }
            .debounce(CLIENT_REBUILD_SETTLE)
            .onEach { checkConnection() }
            .launchIn(scope)
        auth.drop(1).onEach { _signIn.value = SignInState.Idle }.launchIn(scope)
        checkConnection()
    }

    private companion object {
        val CLIENT_REBUILD_SETTLE = 400.milliseconds
        val SIGN_IN_AUTH = setOf(Auth.UI, Auth.SIMPLE)
    }
}

@Composable
fun SettingsServerScreenContent(vm: SettingsServerViewModel) {
    val connection by vm.connection.collectAsState()
    val address by vm.serverAddress.collectAsState()
    val authValue by vm.auth.collectAsState()
    val proxyValue by vm.proxy.collectAsState()
    val username by vm.authUsername.collectAsState()
    val password by vm.authPassword.collectAsState()
    val hasSession by vm.hasSession.collectAsState()
    val signIn by vm.signIn.collectAsState()
    val serverSettings by vm.serverSettings.collectAsState()
    val proxyChoices = vm.getProxyChoices()
    val authChoices = vm.getAuthChoices()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.settings_server_screen))
        },
    ) {
        Box(Modifier.padding(it)) {
            val state = rememberLazyListState()
            val bottomInsets = WindowInsets.bottomNav.add(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = bottomInsets.asPaddingValues(),
            ) {
                item { PreferenceSectionHeader(stringResource(MR.strings.settings_section_connection)) }
                item { ConnectionStatusCard(connection, onRetry = vm::checkConnection) }
                item {
                    ServerAddressForm(
                        saved = address,
                        checking = connection is ConnectionStatus.Checking,
                        onSave = vm::saveAddress,
                        onTest = vm::checkConnection,
                    )
                }

                item { PreferenceSectionHeader(stringResource(MR.strings.authentication)) }
                item { ChoicePreference(vm.auth, authChoices, stringResource(MR.strings.auth_method)) }
                if (authValue != Auth.NONE) {
                    item {
                        AuthForm(
                            auth = authValue,
                            savedUsername = username,
                            savedPassword = password,
                            hasSession = hasSession,
                            status = connection,
                            signIn = signIn,
                            onSignIn = vm::onSignIn,
                            onSignOut = vm::onSignOut,
                        )
                    }
                }

                item { PreferenceSectionHeader(stringResource(MR.strings.settings_section_proxy)) }
                item { ChoicePreference(vm.proxy, proxyChoices, stringResource(MR.strings.server_proxy)) }
                proxyItems(vm, proxyValue)

                item { PreferenceSectionHeader(stringResource(MR.strings.host_settings)) }
                when (val settings = serverSettings) {
                    is ServerSettingsState.Loaded -> ServerSettingsItems(settings.settings)

                    ServerSettingsState.Unavailable -> item {
                        PreferenceRow(
                            stringResource(MR.strings.server_settings_unavailable),
                            subtitle = stringResource(MR.strings.server_settings_unavailable_sub),
                            enabled = false,
                        )
                    }

                    ServerSettingsState.Loading -> item {
                        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(state),
                Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .windowInsetsPadding(bottomInsets),
            )
        }
    }
}

private fun LazyListScope.proxyItems(
    vm: SettingsServerViewModel,
    proxy: Proxy,
) {
    when (proxy) {
        Proxy.NO_PROXY -> {
            Unit
        }

        Proxy.HTTP_PROXY -> {
            item { EditTextPreference(vm.httpHost, stringResource(MR.strings.http_proxy), vm.httpHost.collectAsState().value) }
            item { EditTextPreference(vm.httpPort, stringResource(MR.strings.http_port), vm.httpPort.collectAsState().value) }
        }

        Proxy.SOCKS_PROXY -> {
            item { EditTextPreference(vm.socksHost, stringResource(MR.strings.socks_proxy), vm.socksHost.collectAsState().value) }
            item { EditTextPreference(vm.socksPort, stringResource(MR.strings.socks_port), vm.socksPort.collectAsState().value) }
        }
    }
}
