/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.extensions.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.bunko.core.lang.getDisplayName
import dev.bunko.domain.extension.model.Extension
import dev.bunko.i18n.MR
import dev.bunko.ui.base.ListSectionHeader
import dev.bunko.ui.extensions.ExtensionUI
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.sources.components.SourceStatusChip
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.components.LoadStateCrossfade
import dev.bunko.uicore.components.SkeletonList
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.dialog.MultiChoiceList
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@Composable
fun ExtensionsScreenContent(
    extensions: ImmutableList<ExtensionUI>,
    isLoading: Boolean,
    installExtension: (Extension) -> Unit,
    updateExtension: (Extension) -> Unit,
    uninstallExtension: (Extension) -> Unit,
) {
    LoadStateCrossfade(isLoading, contentKey = { it }) { loading ->
        ExtensionsList(loading, extensions, installExtension, updateExtension, uninstallExtension)
    }
}

@Composable
private fun ExtensionsList(
    loading: Boolean,
    extensions: ImmutableList<ExtensionUI>,
    installExtension: (Extension) -> Unit,
    updateExtension: (Extension) -> Unit,
    uninstallExtension: (Extension) -> Unit,
) {
    if (loading) {
        SkeletonList(coverHeight = 44.dp, lines = 2, coverAspectRatio = 1f, coverShape = RoundedCornerShape(10.dp))
    } else if (extensions.isEmpty()) {
        EmptyScreen(stringResource(MR.strings.extensions_empty), icon = Icons.Outlined.Extension)
    } else {
        val state = rememberLazyListState()

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = WindowInsets.bottomNav.add(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Bottom,
                    ),
                ).asPaddingValues(),
            ) {
                extensions.forEach { entry ->
                    when (entry) {
                        is ExtensionUI.Header -> stickyHeader(key = entry.header, contentType = "header") {
                            ListSectionHeader(entry.header, Modifier.animateItem())
                        }

                        is ExtensionUI.ExtensionItem -> item(key = entry.extension.pkgName, contentType = "extension") {
                            ExtensionItem(
                                Modifier.animateItem(),
                                entry,
                                onInstallClicked = installExtension,
                                onUpdateClicked = updateExtension,
                                onUninstallClicked = uninstallExtension,
                            )
                        }
                    }
                }
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
fun ExtensionItem(
    modifier: Modifier,
    extensionItem: ExtensionUI.ExtensionItem,
    onInstallClicked: (Extension) -> Unit,
    onUpdateClicked: (Extension) -> Unit,
    onUninstallClicked: (Extension) -> Unit,
) {
    val extension = extensionItem.extension
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageLoaderImage(
            data = extension,
            contentDescription = extension.name,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
            filterQuality = FilterQuality.Medium,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                extension.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SourceStatusChip(extension.lang.uppercase())
                SourceStatusChip("v${extension.versionName}")
                when {
                    extension.obsolete -> SourceStatusChip(
                        stringResource(MR.strings.obsolete),
                        color = MaterialTheme.colorScheme.error,
                    )

                    extension.hasUpdate -> SourceStatusChip(
                        stringResource(MR.strings.update_available),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    extension.installed -> SourceStatusChip(stringResource(MR.strings.installed))
                }
                if (extension.isNsfw) {
                    SourceStatusChip("18+", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (extensionItem.isWorking) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        when {
                            !extension.installed -> MR.strings.extension_installing
                            extension.obsolete -> MR.strings.extension_uninstalling
                            extension.hasUpdate -> MR.strings.extension_updating
                            else -> MR.strings.extension_uninstalling
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        } else {
            val uninstalling = extension.obsolete || (extension.installed && !extension.hasUpdate)
            TextButton(
                onClick = {
                    when {
                        uninstalling -> onUninstallClicked(extension)
                        extension.hasUpdate -> onUpdateClicked(extension)
                        else -> onInstallClicked(extension)
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (uninstalling) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ),
            ) {
                Text(
                    stringResource(
                        when {
                            uninstalling -> MR.strings.action_uninstall
                            extension.hasUpdate -> MR.strings.action_update
                            else -> MR.strings.action_install
                        },
                    ),
                )
            }
        }
    }
}

@Composable
fun LanguageDialog(
    state: DialogState,
    enabledLangs: ImmutableSet<String>,
    availableLangs: ImmutableList<String>,
    setLangs: (Set<String>) -> Unit,
) {
    if (!state.isShowing) return
    val locale = remember { Locale.current }
    val all = stringResource(MR.strings.all)
    val other = stringResource(MR.strings.other)
    val list = remember(availableLangs, all, other) {
        availableLangs.map { lang ->
            lang to when (lang) {
                "all" -> all
                "other" -> other
                else -> Locale(lang).getDisplayName(locale)
            }
        }.sortedWith(
            compareBy<Pair<String, String>> { (lang, _) ->
                when (lang) {
                    "all" -> 1
                    "other" -> 3
                    else -> 2
                }
            }.thenBy(String.CASE_INSENSITIVE_ORDER, Pair<*, String>::second),
        ).toImmutableList()
    }
    var checked by remember { mutableStateOf(enabledLangs.toSet()) }
    AppDialog(
        state = state,
        title = stringResource(MR.strings.language),
        confirm = DialogButton(stringResource(MR.strings.action_ok)) { setLangs(checked) },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        MultiChoiceList(list, checked) { checked = if (it in checked) checked - it else checked + it }
    }
}
