/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.chapter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource

@Composable
fun StorageBadges(
    onDevice: Boolean,
    onServer: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (onDevice) {
            StorageBadge(
                icon = Icons.Rounded.PhoneAndroid,
                text = stringResource(MR.strings.badge_on_device),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        if (onServer) {
            StorageBadge(
                icon = Icons.Rounded.Dns,
                text = stringResource(MR.strings.badge_on_server),
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun StorageBadge(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color,
) {
    Surface(shape = RoundedCornerShape(6.dp), color = container, contentColor = content) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}
