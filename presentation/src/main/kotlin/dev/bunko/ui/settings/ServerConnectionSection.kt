/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.bunko.domain.server.model.Auth
import dev.bunko.domain.server.model.parseServerUrl
import dev.bunko.domain.server.parseFallbackOrigin
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogContentPadding
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.uicore.resources.stringResource
import io.ktor.http.Url

data class ServerAddress(
    val url: String,
    val port: String,
    val pathPrefix: String,
    val fallback: String,
) {
    val portNumber: Int?
        get() = port.trim().let { if (it.isEmpty()) 0 else it.toIntOrNull()?.takeIf { p -> p in 1..65535 } }

    val resolved: Url? get() = portNumber?.let { parseServerUrl(url, it, pathPrefix.trim()) }

    val fallbackValid: Boolean
        get() = fallback.isBlank() ||
            resolved?.let { parseFallbackOrigin(fallback, it.protocol.name, it.port) } != null

    val isValid: Boolean get() = resolved != null && fallbackValid
}

private val CardPadding = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)

@Composable
fun ConnectionStatusCard(
    status: ConnectionStatus,
    onRetry: () -> Unit,
) {
    var details by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    val error = (status as? ConnectionStatus.Failed)?.error ?: (status as? ConnectionStatus.SignInRequired)?.error

    Surface(CardPadding.animateContentSize(), shape = MaterialTheme.shapes.large, color = colors.surfaceContainer) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIndicator(status)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(statusTitle(status), style = MaterialTheme.typography.titleMedium)
                    Text(
                        statusSubtitle(status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (status is ConnectionStatus.Connected) {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(MR.strings.conn_test))
                    }
                }
            }
            if (error != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { details = error.detail }) {
                        Text(stringResource(MR.strings.conn_details))
                    }
                    TextButton(onClick = onRetry) {
                        Text(stringResource(MR.strings.action_retry))
                    }
                }
            } else {
                Spacer(Modifier.size(8.dp))
            }
        }
    }

    details?.let { text ->
        AppDialog(
            state = remember { DialogState(showing = true) },
            title = stringResource(MR.strings.conn_details),
            dismissText = stringResource(MR.strings.action_close),
            onDismiss = { details = null },
        ) {
            SelectionContainer(DialogContentPadding) { Text(text, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun StatusIndicator(status: ConnectionStatus) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        ConnectionStatus.Checking -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
        is ConnectionStatus.Connected -> StatusIcon(Icons.Rounded.CheckCircle, colors.primary)
        is ConnectionStatus.SignInRequired -> StatusIcon(Icons.Rounded.Lock, colors.tertiary)
        ConnectionStatus.InvalidAddress -> StatusIcon(Icons.Rounded.ErrorOutline, colors.error)
        is ConnectionStatus.Failed -> StatusIcon(Icons.Rounded.CloudOff, colors.error)
    }
}

@Composable
private fun StatusIcon(
    icon: ImageVector,
    tint: Color,
) = Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))

@Composable
private fun statusTitle(status: ConnectionStatus): String =
    stringResource(
        when (status) {
            ConnectionStatus.Checking -> MR.strings.conn_status_checking
            is ConnectionStatus.Connected -> MR.strings.conn_status_connected
            is ConnectionStatus.SignInRequired -> MR.strings.conn_status_sign_in
            ConnectionStatus.InvalidAddress -> MR.strings.conn_status_invalid
            is ConnectionStatus.Failed -> MR.strings.conn_status_failed
        },
    )

@Composable
private fun statusSubtitle(status: ConnectionStatus): String =
    when (status) {
        ConnectionStatus.Checking -> stringResource(MR.strings.conn_status_checking_sub)
        is ConnectionStatus.Connected -> stringResource(MR.strings.conn_status_connected_sub, status.serverVersion)
        is ConnectionStatus.SignInRequired -> stringResource(MR.strings.conn_status_sign_in_sub)
        ConnectionStatus.InvalidAddress -> stringResource(MR.strings.server_url_invalid)
        is ConnectionStatus.Failed -> stringResource(status.error.message)
    }

@Composable
fun ServerAddressForm(
    saved: ServerAddress,
    checking: Boolean,
    onSave: (ServerAddress) -> Unit,
    onTest: () -> Unit,
) {
    var url by rememberSaveable(saved) { mutableStateOf(saved.url) }
    var port by rememberSaveable(saved) { mutableStateOf(saved.port) }
    var pathPrefix by rememberSaveable(saved) { mutableStateOf(saved.pathPrefix) }
    var fallback by rememberSaveable(saved) { mutableStateOf(saved.fallback) }
    val draft = ServerAddress(url, port, pathPrefix, fallback)
    val dirty = draft != saved
    val resolved = draft.resolved

    Column(CardPadding, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(MR.strings.server_url)) },
            placeholder = { Text(stringResource(MR.strings.server_url_hint)) },
            singleLine = true,
            isError = draft.portNumber != null && resolved == null,
            supportingText = {
                Text(
                    when {
                        draft.portNumber == null -> ""
                        resolved == null -> stringResource(MR.strings.server_url_invalid)
                        else -> stringResource(MR.strings.server_url_resolved, resolved.toString())
                    },
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(MR.strings.server_port_label)) },
                placeholder = { Text(stringResource(MR.strings.server_port_auto)) },
                singleLine = true,
                isError = draft.portNumber == null,
                supportingText = if (draft.portNumber == null) {
                    { Text(stringResource(MR.strings.server_port_invalid)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = pathPrefix,
                onValueChange = { pathPrefix = it },
                modifier = Modifier.weight(2f),
                label = { Text(stringResource(MR.strings.server_path_prefix_label)) },
                placeholder = { Text(stringResource(MR.strings.optional)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            )
        }
        OutlinedTextField(
            value = fallback,
            onValueChange = { fallback = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(MR.strings.server_fallback_url_label)) },
            placeholder = { Text(stringResource(MR.strings.optional)) },
            singleLine = true,
            isError = !draft.fallbackValid,
            supportingText = {
                Text(
                    stringResource(
                        if (draft.fallbackValid) MR.strings.server_fallback_url_sub else MR.strings.server_fallback_url_invalid,
                    ),
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (dirty) {
                TextButton(onClick = {
                    url = saved.url
                    port = saved.port
                    pathPrefix = saved.pathPrefix
                    fallback = saved.fallback
                }) {
                    Text(stringResource(MR.strings.conn_reset))
                }
                Button(onClick = { onSave(draft) }, enabled = draft.isValid) {
                    Text(stringResource(MR.strings.conn_save))
                }
            } else {
                OutlinedButton(onClick = onTest, enabled = !checking && draft.isValid) {
                    Text(stringResource(MR.strings.conn_test))
                }
            }
        }
    }
}

@Composable
fun AuthForm(
    auth: Auth,
    savedUsername: String,
    savedPassword: String,
    hasSession: Boolean,
    status: ConnectionStatus,
    signIn: SignInState,
    onSignIn: (username: String, password: String) -> Unit,
    onSignOut: () -> Unit,
) {
    val usesSession = auth == Auth.SIMPLE || auth == Auth.UI
    val rejected = status is ConnectionStatus.SignInRequired

    if (usesSession && hasSession && !rejected) {
        SignedInRow(savedUsername, onSignOut)
        return
    }

    var username by rememberSaveable(savedUsername) { mutableStateOf(savedUsername) }
    var password by rememberSaveable(savedPassword) { mutableStateOf(savedPassword) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val busy = signIn is SignInState.InProgress || status is ConnectionStatus.Checking
    val canSubmit = !busy && username.isNotBlank() && password.isNotEmpty()
    val submit = { if (canSubmit) onSignIn(username.trim(), password) }

    val errorText = when {
        signIn is SignInState.Failed -> stringResource(signIn.error.message)

        rejected && !usesSession && savedUsername.isNotBlank() -> stringResource(MR.strings.conn_error_unauthorized)

        rejected && hasSession -> stringResource(MR.strings.conn_session_expired)

        else -> null
    }

    Column(CardPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
            label = { Text(stringResource(MR.strings.auth_username)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
            label = { Text(stringResource(MR.strings.auth_password)) },
            singleLine = true,
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it) } },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = stringResource(
                            if (showPassword) MR.strings.hide_password else MR.strings.show_password,
                        ),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = submit, enabled = canSubmit) {
                if (signIn is SignInState.InProgress) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(if (usesSession) MR.strings.sign_in else MR.strings.conn_save))
            }
        }
    }
}

@Composable
private fun SignedInRow(
    username: String,
    onSignOut: () -> Unit,
) {
    Surface(CardPadding, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(MR.strings.signed_in), style = MaterialTheme.typography.titleSmall)
                if (username.isNotBlank()) {
                    Text(username, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onSignOut) { Text(stringResource(MR.strings.sign_out)) }
        }
    }
}
