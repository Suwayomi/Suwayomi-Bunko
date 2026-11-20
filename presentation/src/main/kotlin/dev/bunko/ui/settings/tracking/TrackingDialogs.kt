/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogContentPadding
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.uicore.resources.stringResource

// shown by the caller, which also decides when a successful confirm closes them
@Composable
private fun rememberShownDialog() = remember { DialogState(showing = true) }

@Composable
internal fun CredentialsDialog(
    trackerName: String,
    onDismiss: () -> Unit,
    onConfirm: (username: String, password: String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AppDialog(
        state = rememberShownDialog(),
        title = stringResource(MR.strings.tracker_login_title, trackerName),
        confirm = DialogButton(
            stringResource(MR.strings.tracker_login),
            enabled = username.isNotBlank() && password.isNotEmpty(),
        ) { onConfirm(username, password) },
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onDismiss,
        dismissOnConfirm = false,
    ) {
        Column(DialogContentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(MR.strings.tracker_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(MR.strings.tracker_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun LogoutDialog(
    trackerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppDialog(
        state = rememberShownDialog(),
        title = stringResource(MR.strings.tracker_logout_confirm, trackerName),
        confirm = DialogButton(stringResource(MR.strings.tracker_logout), onClick = onConfirm),
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onDismiss,
        dismissOnConfirm = false,
    ) {}
}

@Composable
internal fun DiscordTokenDialog(
    checking: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var token by remember { mutableStateOf("") }
    AppDialog(
        state = rememberShownDialog(),
        title = stringResource(MR.strings.discord_token),
        confirm = DialogButton(
            stringResource(MR.strings.discord_link),
            enabled = token.isNotEmpty() && !checking,
        ) { onConfirm(token) },
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onDismiss,
        dismissOnConfirm = false,
    ) {
        Column(DialogContentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(MR.strings.discord_token_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it.trim() },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (checking) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
