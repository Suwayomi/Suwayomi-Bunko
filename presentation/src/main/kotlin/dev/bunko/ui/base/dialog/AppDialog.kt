/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Stable
class DialogState(showing: Boolean = false) {
    var isShowing by mutableStateOf(showing)
        private set

    fun show() {
        isShowing = true
    }

    fun hide() {
        isShowing = false
    }

    companion object {
        val Saver = Saver<DialogState, Boolean>(save = { it.isShowing }, restore = ::DialogState)
    }
}

@Composable
fun rememberDialogState(): DialogState = rememberSaveable(saver = DialogState.Saver) { DialogState() }

val DialogContentPadding = Modifier.padding(horizontal = 24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDialog(
    state: DialogState,
    title: String?,
    confirm: DialogButton? = null,
    dismissText: String? = null,
    onDismiss: () -> Unit = {},
    neutral: DialogButton? = null,
    dismissOnConfirm: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!state.isShowing) return
    val dismiss = {
        state.hide()
        onDismiss()
    }
    BasicAlertDialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp).widthIn(min = 280.dp, max = 560.dp),
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                if (title != null) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlertDialogDefaults.titleContentColor,
                        modifier = DialogContentPadding.padding(bottom = 16.dp),
                    )
                }
                Column(Modifier.weight(1f, fill = false)) {
                    CompositionLocalProvider(
                        LocalContentColor provides AlertDialogDefaults.textContentColor,
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                        content = { content() },
                    )
                }
                if (confirm != null || dismissText != null || neutral != null) {
                    DialogButtons(
                        neutral = neutral,
                        dismissText = dismissText,
                        onDismiss = dismiss,
                        confirm = if (dismissOnConfirm) confirm?.let { it.copy(onClick = { it.onClick(); state.hide() }) } else confirm,
                    )
                }
            }
        }
    }
}

data class DialogButton(
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {},
)

@Composable
private fun DialogButtons(
    neutral: DialogButton?,
    dismissText: String?,
    onDismiss: () -> Unit,
    confirm: DialogButton?,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (neutral != null) {
            TextButton(onClick = neutral.onClick, enabled = neutral.enabled) { Text(neutral.text) }
        }
        Spacer(Modifier.weight(1f))
        if (dismissText != null) {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        }
        if (confirm != null) {
            TextButton(onClick = confirm.onClick, enabled = confirm.enabled) { Text(confirm.text) }
        }
    }
}
