/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.about.components

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.bunko.domain.settings.model.About
import dev.bunko.domain.updates.interactor.UpdateChecker
import dev.bunko.i18n.MR
import dev.bunko.presentation.BuildConfig
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.resources.toPainter
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.launch

@Composable
fun AboutContent(
    about: About?,
    formattedBuildTime: String,
    checkForUpdates: () -> Unit,
    openSourceLicenses: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.location_about))
        },
    ) {
        Box(Modifier.padding(it)) {
            val state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                contentPadding = WindowInsets.bottomNav.add(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Bottom,
                    ),
                ).asPaddingValues(),
            ) {
                item {
                    IconImage()
                }
                item {
                    HorizontalDivider()
                }
                item {
                    CheckForUpdates(checkForUpdates)
                }
                item {
                    ClientVersionInfo()
                }
                item {
                    ServerVersionInfo(about, formattedBuildTime)
                }
                item {
                    WhatsNew()
                }
                item {
                    OpenSourceLicenses(openSourceLicenses)
                }
                item {
                    LinkDisplay()
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
private fun IconImage() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            painter = MR.images.icon.toPainter(),
            contentDescription = null,
            modifier = Modifier.height(140.dp).padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun CheckForUpdates(checkForUpdates: () -> Unit) {
    PreferenceRow(
        title = stringResource(MR.strings.update_checker),
        onClick = checkForUpdates,
    )
}

@Composable
private fun ClientVersionInfo() {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    PreferenceRow(
        title = stringResource(MR.strings.version),
        subtitle = BuildConfig.VERSION,
        onClick = {
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, getDebugInfo()))) }
        },
    )
}

@Composable
private fun ServerVersionInfo(
    about: About?,
    formattedBuildTime: String,
) {
    if (about == null) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        PreferenceRow(
            title = stringResource(MR.strings.server_version),
            subtitle = "${about.buildType.name} ${about.version} ($formattedBuildTime)",
        )
    }
}

@Composable
private fun WhatsNew() {
    val uriHandler = LocalUriHandler.current
    PreferenceRow(
        title = stringResource(MR.strings.whats_new),
        onClick = {
            uriHandler.openUri(UpdateChecker.RELEASE_URL)
        },
    )
}

@Composable
private fun OpenSourceLicenses(openSourceLicenses: () -> Unit) {
    PreferenceRow(
        title = stringResource(MR.strings.open_source_licenses),
        onClick = openSourceLicenses,
    )
}

sealed class LinkIcon {
    data class Resource(
        val res: ImageResource,
    ) : LinkIcon()

    data class Icon(
        val icon: ImageVector,
    ) : LinkIcon()
}

enum class Link(
    val nameRes: StringResource,
    val icon: LinkIcon,
    val uri: String,
) {
    Github(MR.strings.github, LinkIcon.Resource(MR.images.github), "https://github.com/Suwayomi/Suwayomi-Bunko"),
}

@Composable
private fun LinkDisplay() {
    val uriHandler = LocalUriHandler.current
    BoxWithConstraints {
        val wide = maxWidth > 720.dp
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (wide) {
                Link.entries.fastForEach {
                    Column(
                        Modifier
                            .width(92.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { uriHandler.openUri(it.uri) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val name = stringResource(it.nameRes)
                        val modifier = Modifier.size(48.dp)
                        when (it.icon) {
                            is LinkIcon.Resource -> Icon(
                                painter = it.icon.res.toPainter(),
                                contentDescription = name,
                                modifier = modifier,
                            )

                            is LinkIcon.Icon -> Icon(
                                imageVector = it.icon.icon,
                                contentDescription = name,
                                modifier = modifier,
                            )
                        }
                        Text(name)
                    }
                }
            } else {
                Link.entries.fastForEach {
                    Box(
                        modifier = Modifier.clickable { uriHandler.openUri(it.uri) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .size(32.dp),
                    ) {
                        val name = stringResource(it.nameRes)
                        when (it.icon) {
                            is LinkIcon.Resource -> Icon(
                                painter = it.icon.res.toPainter(),
                                contentDescription = name,
                                modifier = Modifier.fillMaxSize(),
                            )

                            is LinkIcon.Icon -> Icon(
                                imageVector = it.icon.icon,
                                contentDescription = name,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getDebugInfo(): String =
    """
        App version: ${BuildConfig.VERSION} (${ if (BuildConfig.DEBUG) "Debug" else "Standard"}, ${BuildConfig.MIGRATION_CODE})
        Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        Android build ID: ${Build.DISPLAY}
        Device brand: ${Build.BRAND}
        Device manufacturer: ${Build.MANUFACTURER}
        Device name: ${Build.DEVICE}
        Device model: ${Build.MODEL}
        Device product name: ${Build.PRODUCT}
    """.trimIndent()
