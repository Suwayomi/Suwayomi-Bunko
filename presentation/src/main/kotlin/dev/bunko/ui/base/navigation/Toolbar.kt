/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.uicore.components.keyboardHandler
import dev.bunko.uicore.resources.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

val ToolbarDefault = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    name: String,
    navigator: Navigator? = LocalNavigator.current,
    closable: Boolean = (navigator?.size ?: 0) > 1,
    onClose: () -> Unit = { navigator?.pop() },
    closeIcon: ImageVector = ToolbarDefault,
    modifier: Modifier = Modifier,
    actions: @Composable () -> ImmutableList<Action> = { remember { persistentListOf() } },
    searchText: String? = null,
    search: ((String) -> Unit)? = null,
    searchSubmit: (() -> Unit)? = null,
    closeSearchOnBack: Boolean = true,
    isActionMode: Boolean = false,
    containerColor: Color? = null,
    titleAlpha: Float = 1f,
) {
    var searchMode by remember { mutableStateOf(!searchText.isNullOrEmpty()) }

    fun closeSearch() {
        search?.invoke("")
        searchSubmit?.invoke()
        searchMode = false
    }
    BackHandler(searchMode && closeSearchOnBack, ::closeSearch)

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor ?: if (isActionMode) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        title = {
            if (searchMode) {
                SearchField(searchText.orEmpty(), search ?: {}, searchSubmit)
            } else {
                Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.graphicsLayer { alpha = titleAlpha })
            }
        },
        navigationIcon = {
            if (closable || searchMode) {
                IconButton(onClick = { if (searchMode && closeSearchOnBack) closeSearch() else onClose() }) {
                    Icon(
                        if (closeIcon === ToolbarDefault) Icons.AutoMirrored.Rounded.ArrowBack else closeIcon,
                        stringResource(MR.strings.action_close),
                    )
                }
            }
        },
        actions = {
            if (search != null) {
                if (!searchMode) {
                    IconButton(onClick = { searchMode = true }) {
                        Icon(Icons.Rounded.Search, stringResource(MR.strings.action_search))
                    }
                } else if (!searchText.isNullOrEmpty()) {
                    IconButton(onClick = { search("") }) {
                        Icon(Icons.Rounded.Close, stringResource(MR.strings.action_close))
                    }
                }
            }
            ActionMenu(
                actions(),
                if (searchMode) 1 else 3,
            ) { onClick: () -> Unit, name: String, icon: ImageVector, enabled: Boolean ->
                IconButton(onClick = onClick, enabled = enabled) {
                    Icon(icon, name)
                }
            }
        },
    )
}

@Composable
private fun SearchField(
    text: String,
    onChange: (String) -> Unit,
    onSubmit: (() -> Unit)?,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    BasicTextField(
        value = text,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth()
            .focusRequester(focusRequester)
            .keyboardHandler(singleLine = true) {
                onSubmit?.invoke()
                it.clearFocus()
            },
        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardActions = KeyboardActions {
            onSubmit?.invoke()
            focusManager.clearFocus()
        },
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorationBox = { innerTextField ->
            if (text.isEmpty()) {
                Text(
                    stringResource(MR.strings.action_searching),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            innerTextField()
        },
    )
}
