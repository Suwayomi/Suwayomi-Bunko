/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.categories.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.dialog.DialogTextField
import dev.bunko.ui.categories.CategoriesScreenViewModel
import dev.bunko.uicore.resources.stringResource

@Composable
fun RenameDialog(
    state: DialogState,
    category: CategoriesScreenViewModel.MenuCategory,
    onRename: (String) -> Unit,
) {
    NameDialog(state, stringResource(MR.strings.categories_rename), stringResource(MR.strings.action_rename), category.name, onRename)
}

@Composable
fun DeleteDialog(
    state: DialogState,
    category: CategoriesScreenViewModel.MenuCategory,
    onDelete: (CategoriesScreenViewModel.MenuCategory) -> Unit,
) {
    AppDialog(
        state = state,
        title = stringResource(MR.strings.categories_delete),
        confirm = DialogButton(stringResource(MR.strings.action_yes)) { onDelete(category) },
        dismissText = stringResource(MR.strings.action_no),
    ) {
        DialogMessage(stringResource(MR.strings.categories_delete_confirm, category.name))
    }
}

@Composable
fun CreateDialog(
    state: DialogState,
    onCreate: (String) -> Unit,
) {
    NameDialog(state, stringResource(MR.strings.categories_create), stringResource(MR.strings.action_create), "", onCreate)
}

@Composable
private fun NameDialog(
    state: DialogState,
    title: String,
    confirmText: String,
    initial: String,
    onConfirm: (String) -> Unit,
) {
    if (!state.isShowing) return
    var name by remember { mutableStateOf(initial) }
    AppDialog(
        state = state,
        title = title,
        confirm = DialogButton(confirmText, enabled = name.isNotBlank()) { onConfirm(name.trim()) },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        DialogTextField(value = name, onValueChange = { name = it })
    }
}
