/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.ChoiceDialog
import dev.bunko.ui.base.prefs.MultiSelectDialog
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.sources.settings.model.SourceSettingsView
import dev.bunko.ui.sources.settings.model.SourceSettingsView.CheckBox
import dev.bunko.ui.sources.settings.model.SourceSettingsView.EditText
import dev.bunko.ui.sources.settings.model.SourceSettingsView.List
import dev.bunko.ui.sources.settings.model.SourceSettingsView.MultiSelect
import dev.bunko.ui.sources.settings.model.SourceSettingsView.Switch
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.DialogTextField
import dev.bunko.ui.base.dialog.rememberDialogState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

@Composable
fun SourceSettingsScreenContent(settings: ImmutableList<SourceSettingsView<*, *>>) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.location_settings))
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
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
                items(settings, key = { it.index }, contentType = { it::class }) {
                    when (it) {
                        is CheckBox, is Switch -> {
                            TwoStatePreference(it, it is CheckBox)
                        }

                        is List -> {
                            ListPreference(it)
                        }

                        is EditText -> {
                            EditTextPreference(it)
                        }

                        is MultiSelect -> {
                            MultiSelectPreference(it)
                        }
                    }
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
}

@Composable
private fun TwoStatePreference(
    twoState: SourceSettingsView<*, Boolean>,
    checkbox: Boolean,
) {
    val state by twoState.state.collectAsState()
    val title = remember(state) { twoState.title ?: twoState.summary ?: "No title" }
    val subtitle = remember(state) {
        if (twoState.title == null) {
            null
        } else {
            twoState.summary
        }
    }
    PreferenceRow(
        title,
        subtitle = subtitle,
        onClick = { twoState.updateState(!state) },
        action = {
            if (checkbox) {
                Checkbox(checked = state, onCheckedChange = null)
            } else {
                Switch(checked = state, onCheckedChange = null)
            }
        },
    )
}

@Composable
private fun ListPreference(list: List) {
    val state by list.state.collectAsState()
    val title = remember(state) { list.title ?: list.summary ?: "No title" }
    val subtitle = remember(state) {
        if (list.title == null) {
            null
        } else {
            list.summary
        }
    }
    val dialogState = rememberDialogState()
    PreferenceRow(
        title,
        subtitle = subtitle,
        onClick = {
            dialogState.show()
        },
    )
    ChoiceDialog(
        dialogState,
        list.getOptions(),
        state,
        onSelected = list::updateState,
        title = title,
    )
}

@Composable
private fun MultiSelectPreference(multiSelect: MultiSelect) {
    val state by multiSelect.state.collectAsState()
    val title = remember(state) { multiSelect.title ?: multiSelect.summary ?: "No title" }
    val subtitle = remember(state) {
        if (multiSelect.title == null) {
            null
        } else {
            multiSelect.summary
        }
    }
    val dialogTitle = remember(state) { multiSelect.props.dialogTitle ?: multiSelect.title ?: multiSelect.summary ?: "No title" }
    val dialogState = rememberDialogState()
    PreferenceRow(
        title,
        subtitle = subtitle,
        onClick = {
            dialogState.show()
        },
    )
    MultiSelectDialog(
        dialogState,
        multiSelect.getOptions(),
        state,
        onFinished = multiSelect::updateState,
        title = dialogTitle,
    )
}

@Composable
private fun EditTextPreference(editText: EditText) {
    val state by editText.state.collectAsState()
    val title = remember(state) { editText.title ?: editText.summary ?: "No title" }
    val subtitle = remember(state) {
        if (editText.title == null) {
            null
        } else {
            editText.summary
        }
    }
    val dialogState = rememberDialogState()
    PreferenceRow(
        title,
        subtitle = subtitle,
        onClick = dialogState::show,
    )
    if (!dialogState.isShowing) return
    var text by remember { mutableStateOf(state) }
    AppDialog(
        state = dialogState,
        title = editText.dialogTitle ?: title,
        confirm = DialogButton(stringResource(MR.strings.action_ok)) { editText.updateState(text) },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        if (editText.dialogMessage != null) {
            DialogMessage(editText.dialogMessage)
            Spacer(Modifier.height(16.dp))
        }
        DialogTextField(value = text, onValueChange = { text = it })
    }
}
