/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import dev.bunko.uicore.theme.BunkoMotion
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource

@Composable
fun AppStateBanners(
    offline: Boolean,
    onDeviceOnly: Boolean,
    incognito: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var offlineHeight by remember { mutableStateOf(0.dp) }
    var onDeviceHeight by remember { mutableStateOf(0.dp) }
    Column(modifier) {
        StateBanner(
            visible = offline,
            icon = Icons.Rounded.CloudOff,
            text = stringResource(MR.strings.offline_banner),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.onSizeChanged { offlineHeight = with(density) { it.height.toDp() } },
        )
        StateBanner(
            visible = onDeviceOnly && !offline,
            icon = Icons.Rounded.OfflinePin,
            text = stringResource(MR.strings.library_on_device_banner),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .consumeWindowInsets(PaddingValues(top = offlineHeight))
                .onSizeChanged { onDeviceHeight = with(density) { it.height.toDp() } },
        )
        StateBanner(
            visible = incognito,
            icon = Icons.Rounded.VisibilityOff,
            text = stringResource(MR.strings.incognito_banner),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.consumeWindowInsets(PaddingValues(top = offlineHeight + onDeviceHeight)),
        )
    }
}

@Composable
private fun StateBanner(
    visible: Boolean,
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(tween(BunkoMotion.LONG_MS, easing = BunkoMotion.EmphasizedDecelerate)),
        exit = shrinkVertically(tween(BunkoMotion.MEDIUM_MS, easing = BunkoMotion.EmphasizedAccelerate)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .windowInsetsPadding(
                    WindowInsets.statusBars.add(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}
