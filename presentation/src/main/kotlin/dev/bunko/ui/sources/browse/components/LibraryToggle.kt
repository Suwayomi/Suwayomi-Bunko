/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.manga.model.Manga
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.manga.components.CategorySelectDialog
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Long-press handler for browsed titles: asks before removing one from the library, and offers the category
 * picker when adding if any categories exist. Returns the handler to attach to each item.
 */
@Composable
fun rememberLibraryToggle(
    categories: ImmutableList<Category>,
    onAdd: (Manga, List<Category>) -> Unit,
    onRemove: (Manga) -> Unit,
): (Manga) -> Unit {
    // the dialogs live exactly as long as the pressed title they act on
    var pressed by remember { mutableStateOf<Manga?>(null) }
    val categoryDialog = remember { DialogState() }
    val removeDialog = remember { DialogState() }

    CategorySelectDialog(categoryDialog, categories, persistentListOf()) { picked, _ ->
        pressed?.let { onAdd(it, picked) }
    }
    AppDialog(
        state = removeDialog,
        title = null,
        confirm = DialogButton(stringResource(MR.strings.action_remove)) { pressed?.let(onRemove) },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        DialogMessage(stringResource(MR.strings.remove_title_from_library_confirm, pressed?.title.orEmpty()))
    }

    return { manga ->
        pressed = manga
        when {
            manga.inLibrary -> removeDialog.show()
            categories.isEmpty() -> onAdd(manga, emptyList())
            else -> categoryDialog.show()
        }
    }
}
