/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.prefs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.dialog.DialogTextField
import dev.bunko.ui.base.dialog.MultiChoiceList
import dev.bunko.ui.base.dialog.SingleChoiceList
import dev.bunko.ui.base.dialog.rememberDialogState
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PreferenceRow(
    title: String,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    subtitle: String? = null,
    enabled: Boolean = true,
    action: @Composable (BoxScope.() -> Unit)? = null,
) {
    val height = if (subtitle != null) 72.dp else 56.dp

    var modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = height)
    if (enabled) {
        modifier = modifier.combinedClickable(
            onLongClick = onLongClick,
            onClick = onClick,
        )
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                modifier = Modifier.padding(start = 16.dp).size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
        }
        Column(Modifier.padding(horizontal = 16.dp).weight(1f)) {
            Text(
                text = title,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (action != null) {
            Box(Modifier.widthIn(min = 56.dp)) {
                action()
            }
        }
    }
}

@Composable
fun SwitchPreference(
    preference: MutableStateFlow<Boolean>,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    changeListener: () -> Unit = {},
    enabled: Boolean = true,
) {
    PreferenceRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        action = {
            val prefValue by preference.collectAsState()
            Switch(checked = prefValue, onCheckedChange = null, enabled = enabled)
        },
        onClick = {
            preference.value = !preference.value
            changeListener()
        },
        enabled = enabled,
    )
}

@Composable
fun EditTextPreference(
    preference: MutableStateFlow<String>,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    changeListener: () -> Unit = {},
    enabled: Boolean = true,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val dialogState = rememberDialogState()
    PreferenceRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = dialogState::show,
        enabled = enabled,
    )
    if (dialogState.isShowing) {
        var text by remember { mutableStateOf(preference.value) }
        AppDialog(
            state = dialogState,
            title = title,
            confirm = DialogButton(stringResource(MR.strings.action_ok)) {
                preference.value = text
                changeListener()
            },
            dismissText = stringResource(MR.strings.action_cancel),
        ) {
            DialogTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = singleLine,
                maxLines = maxLines,
                visualTransformation = visualTransformation,
            )
        }
    }
}

@Composable
fun <Key> ChoicePreference(
    preference: MutableStateFlow<Key>,
    choices: ImmutableMap<Key, String>,
    title: String,
    subtitle: String? = null,
    changeListener: () -> Unit = {},
    enabled: Boolean = true,
) {
    val prefValue by preference.collectAsState()
    val dialogState = rememberDialogState()
    PreferenceRow(
        title = title,
        subtitle = subtitle ?: choices[prefValue],
        onClick = dialogState::show,
        enabled = enabled,
    )
    ChoiceDialog(
        state = dialogState,
        items = choices.toList().toImmutableList(),
        selected = prefValue,
        title = title,
        onSelected = { selected ->
            preference.value = selected
            changeListener()
        },
    )
}

@Composable
fun <T> ChoiceDialog(
    state: DialogState,
    items: ImmutableList<Pair<T, String>>,
    selected: T?,
    onCloseRequest: () -> Unit = {},
    onSelected: (T) -> Unit,
    title: String,
) {
    AppDialog(
        state = state,
        title = title,
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onCloseRequest,
    ) {
        SingleChoiceList(items, selected) {
            onSelected(it)
            state.hide()
        }
    }
}

@Composable
fun <T> MultiSelectDialog(
    state: DialogState,
    items: ImmutableList<Pair<T, String>>,
    selected: ImmutableList<T>?,
    onCloseRequest: () -> Unit = {},
    onFinished: (ImmutableList<T>) -> Unit,
    title: String,
) {
    if (!state.isShowing) return
    var checked by remember { mutableStateOf(selected.orEmpty().toSet()) }
    AppDialog(
        state = state,
        title = title,
        confirm = DialogButton(stringResource(MR.strings.action_ok)) {
            onFinished(items.map { it.first }.filter { it in checked }.toImmutableList())
        },
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onCloseRequest,
    ) {
        MultiChoiceList(items, checked) { checked = if (it in checked) checked - it else checked + it }
    }
}

@Composable
fun ColorPreference(
    preference: MutableStateFlow<Color>,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    unsetColor: Color = Color.Unspecified,
) {
    val initialColor = preference.value.takeOrElse { unsetColor }
    val dialogState = rememberDialogState()
    PreferenceRow(
        title = title,
        subtitle = subtitle,
        onClick = dialogState::show,
        onLongClick = { preference.value = Color.Unspecified },
        action = {
            val prefValue by preference.collectAsState()
            if (prefValue != Color.Unspecified || unsetColor != Color.Unspecified) {
                val borderColor = MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color = initialColor)
                        .border(BorderStroke(1.dp, borderColor), CircleShape),
                )
            }
        },
        enabled = enabled,
    )
    ColorPickerDialog(
        state = dialogState,
        title = title,
        onSelected = {
            preference.value = it
        },
        initialColor = initialColor,
    )
}
