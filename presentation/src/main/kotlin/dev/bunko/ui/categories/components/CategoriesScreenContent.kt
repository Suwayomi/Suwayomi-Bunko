/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.categories.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.categories.CategoriesScreenViewModel.MenuCategory
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource
import com.diamondedge.logging.logging
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import dev.bunko.ui.base.dialog.rememberDialogState

private val log = logging()

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun CategoriesScreenContent(
    categories: ImmutableList<MenuCategory>,
    updateRemoteCategories: suspend () -> Unit,
    moveCategoryUp: (MenuCategory) -> Unit,
    moveCategoryDown: (MenuCategory) -> Unit,
    renameCategory: (MenuCategory, String) -> Unit,
    deleteCategory: (MenuCategory) -> Unit,
    createCategory: (String) -> Unit,
    notifyFinished: (() -> Unit)? = null,
) {
    DisposableEffect(Unit) {
        onDispose {
            val handler = CoroutineExceptionHandler { _, throwable ->
                log.warn(throwable) { "Error updating remote categories" }
            }
            GlobalScope.launch(handler) {
                updateRemoteCategories()
                notifyFinished?.invoke()
            }
        }
    }

    val createDialogState = rememberDialogState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(
                stringResource(MR.strings.location_categories),
            )
        },
    ) {
        Box(Modifier.padding(it)) {
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
                itemsIndexed(categories) { i, category ->
                    val renameDialogState = rememberDialogState()
                    val deleteDialogState = rememberDialogState()
                    CategoryRow(
                        category = category,
                        moveUpEnabled = i != 0,
                        moveDownEnabled = i != categories.lastIndex,
                        onMoveUp = { moveCategoryUp(category) },
                        onMoveDown = { moveCategoryDown(category) },
                        onRename = {
                            renameDialogState.show()
                        },
                        onDelete = {
                            deleteDialogState.show()
                        },
                    )
                    RenameDialog(renameDialogState, category) {
                        renameCategory(category, it)
                    }
                    DeleteDialog(deleteDialogState, category) {
                        deleteCategory(category)
                    }
                }
                item {
                    Spacer(Modifier.height(80.dp).fillMaxWidth())
                }
            }
            if (categories.isEmpty()) {
                Text(
                    text = stringResource(MR.strings.categories_empty),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(MR.strings.categories_add)) },
                icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        WindowInsets.bottomNav.add(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                        ),
                    )
                    .padding(16.dp),
                onClick = {
                    createDialogState.show()
                },
            )
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
    CreateDialog(createDialogState, createCategory)
}

@Composable
private fun CategoryRow(
    category: MenuCategory,
    moveUpEnabled: Boolean = true,
    moveDownEnabled: Boolean = true,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.List,
                    modifier = Modifier.padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                )
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(MR.strings.categories_items, category.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = moveUpEnabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = moveDownEnabled,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onRename) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}
