/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.about.licenses.components

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import dev.bunko.core.lang.withIOContext
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.LoadingScreen
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.util.withContext

@Composable
fun LicensesContent() {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.open_source_licenses))
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val libs = getLicenses()
            if (libs != null) {
                val state = rememberLazyListState()
                val uriHandler = LocalUriHandler.current
                LibrariesContainer(
                    libraries = libs,
                    lazyListState = state,
                    header = { item { UpstreamCredits() } },
                    onLibraryClick = {
                        it.website?.let(uriHandler::openUri)
                    },
                    contentPadding = WindowInsets.bottomNav.add(
                        WindowInsets.navigationBars.only(
                            WindowInsetsSides.Bottom,
                        ),
                    ).asPaddingValues(),
                )
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
            } else {
                LoadingScreen()
            }
        }
    }
}

@Composable
fun getLicenses(): Libs? {
    val context = LocalContext.current
    val libs by produceState<Libs?>(
        null,
        context,
    ) {
        withIOContext {
            value = Libs.Builder().withContext(context).build()
        }
    }
    return libs
}
