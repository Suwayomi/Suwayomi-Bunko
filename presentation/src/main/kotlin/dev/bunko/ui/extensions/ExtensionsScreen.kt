/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.bunko.ui.sources.BrowseScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class ExtensionsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val browse = remember { BrowseScreen(initialTab = 1) }
        LocalNavigator.currentOrThrow.saveableState("extensionsBrowse", browse) {
            browse.Content()
        }
    }
}
