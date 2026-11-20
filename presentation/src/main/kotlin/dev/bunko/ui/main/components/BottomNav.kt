/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.components

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.bunko.ui.main.TopLevelMenus
import dev.bunko.uicore.resources.stringResource
import cafe.adriel.voyager.navigator.Navigator

private val BottomNavHeight = 80.dp

private val BottomNavHeightLocal = compositionLocalOf { 0.dp }

private class BottomNavHider {
    var hidden by mutableStateOf(false)
}

private val LocalBottomNavHider = staticCompositionLocalOf { BottomNavHider() }

/** Slides the bottom navigation away while [hidden], e.g. so a selection bar can take its place. */
@Composable
fun HideBottomNav(hidden: Boolean) {
    val hider = LocalBottomNavHider.current
    DisposableEffect(hider, hidden) {
        hider.hidden = hidden
        onDispose { hider.hidden = false }
    }
}

val WindowInsets.Companion.bottomNav
    @Composable get() = WindowInsets(bottom = BottomNavHeightLocal.current)

@Composable
fun WithBottomNav(
    navigator: Navigator,
    content: @Composable () -> Unit,
) {
    Box {
        val hider = remember { BottomNavHider() }
        val isBottomNavVisible = navigator.size <= 1 && !hider.hidden
        CompositionLocalProvider(
            BottomNavHeightLocal provides if (isBottomNavVisible) BottomNavHeight else 0.dp,
            LocalBottomNavHider provides hider,
            content = content,
        )
        AnimatedVisibility(
            isBottomNavVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomNav(navigator)
        }
    }
}

@Composable
fun BottomNav(navigator: Navigator) {
    NavigationBar {
        TopLevelMenus.entries.fastForEach {
            val isSelected = it.isSelected(navigator)
            val title = stringResource(it.textKey)
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) navigator replace it.createScreen()
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) it.selectedIcon else it.unselectedIcon,
                        contentDescription = title,
                    )
                },
                label = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .height(BottomNavHeight)
                .selectableGroup(),
            content = content,
        )
    }
}
