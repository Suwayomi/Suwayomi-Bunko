/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.more.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ChromeReaderMode
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.main.MoreMenus
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.categories.CategoriesScreen
import dev.bunko.ui.settings.SettingsAdvancedScreen
import dev.bunko.ui.settings.SettingsAppearanceScreen
import dev.bunko.ui.settings.SettingsBackupScreen
import dev.bunko.ui.settings.SettingsGeneralScreen
import dev.bunko.ui.settings.SettingsLibraryScreen
import dev.bunko.ui.settings.SettingsReaderScreen
import dev.bunko.ui.settings.SettingsServerScreen
import dev.bunko.ui.settings.SettingsTrackingScreen
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.resources.toPainter
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.icerock.moko.resources.StringResource

@Composable
fun MoreContent(
    incognito: Boolean,
    setIncognito: (Boolean) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.location_more))
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
                if (incognito) {
                    item { IncognitoBanner() }
                }
                item {
                    val navigator = LocalNavigator.current
                    MoreHeaderCard(onClick = { navigator?.push(SettingsServerScreen()) })
                }
                item {
                    MoreGroup {
                        IncognitoItem(incognito, setIncognito)
                    }
                }
                item { MoreMenuGroup(listOf(MoreMenus.Downloads, MoreMenus.Offline)) }
                item { MoreShortcuts() }
                item { MoreMenuGroup(listOf(MoreMenus.About)) }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .windowInsetsPadding(
                        WindowInsets.bottomNav.add(
                            WindowInsets.navigationBars.only(
                                WindowInsetsSides.Bottom,
                            ),
                        ),
                    ),
                adapter = rememberScrollbarAdapter(state),
            )
        }
    }
}

@Composable
private fun MoreMenuGroup(menus: List<MoreMenus>) {
    val navigator = LocalNavigator.current
    MoreGroup {
        menus.fastForEach { menu ->
            MoreItem(
                menu = menu,
                description = stringResource(menu.descriptionKey()),
                onClick = { navigator?.push(menu.createScreen()) },
            )
        }
    }
}

@Composable
private fun MoreShortcuts() {
    val navigator = LocalNavigator.current
    val shortcuts = listOf(
        Shortcut(Icons.Rounded.Tune, MR.strings.settings_general, MR.strings.settings_general_desc) { SettingsGeneralScreen() },
        Shortcut(Icons.Rounded.Palette, MR.strings.settings_appearance, MR.strings.settings_appearance_desc) { SettingsAppearanceScreen() },
        Shortcut(Icons.AutoMirrored.Rounded.ChromeReaderMode, MR.strings.settings_reader, MR.strings.settings_reader_desc) { SettingsReaderScreen() },
        Shortcut(Icons.Rounded.CollectionsBookmark, MR.strings.settings_library, MR.strings.settings_library_desc) { SettingsLibraryScreen() },
        Shortcut(Icons.Rounded.Sync, MR.strings.settings_tracking_discord, MR.strings.settings_tracking_desc) { SettingsTrackingScreen() },
        Shortcut(Icons.Rounded.Category, MR.strings.location_categories, MR.strings.more_categories_desc) { CategoriesScreen() },
        Shortcut(Icons.Rounded.Backup, MR.strings.settings_backup_restore, MR.strings.settings_backup_desc) { SettingsBackupScreen() },
        Shortcut(Icons.Rounded.Code, MR.strings.settings_advanced, MR.strings.settings_advanced_desc) { SettingsAdvancedScreen() },
    )
    MoreGroup {
        shortcuts.fastForEach { shortcut ->
            MoreRow(
                icon = shortcut.icon,
                title = stringResource(shortcut.title),
                description = stringResource(shortcut.description),
                onClick = { navigator?.push(shortcut.screen()) },
            )
        }
    }
}

private class Shortcut(
    val icon: ImageVector,
    val title: StringResource,
    val description: StringResource,
    val screen: () -> Screen,
)

@Composable
private fun MoreHeaderCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = MR.images.icon.toPainter(),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(MR.strings.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(MR.strings.more_server_shortcut),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IncognitoBanner() {
    Text(
        text = stringResource(MR.strings.incognito_banner),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun IncognitoItem(
    incognito: Boolean,
    setIncognito: (Boolean) -> Unit,
) {
    MoreRow(
        icon = Icons.Rounded.VisibilityOff,
        title = stringResource(MR.strings.incognito_mode),
        description = stringResource(MR.strings.incognito_mode_desc),
        onClick = { setIncognito(!incognito) },
        trailing = { Switch(incognito, onCheckedChange = setIncognito) },
    )
}

@Composable
private fun MoreGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun MoreItem(
    menu: MoreMenus,
    description: String,
    onClick: () -> Unit,
) {
    MoreRow(
        icon = menu.unselectedIcon,
        title = stringResource(menu.textKey),
        description = description,
        onClick = onClick,
        extra = menu.extraInfo,
    )
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            extra?.invoke()
        }
        trailing?.invoke()
    }
}

private fun MoreMenus.descriptionKey(): StringResource =
    when (this) {
        MoreMenus.Downloads -> MR.strings.location_downloads_desc
        MoreMenus.Offline -> MR.strings.location_offline_desc
        MoreMenus.About -> MR.strings.location_about_desc
    }
