/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource

// covers of titles already in the library fade back so new finds stand out while browsing
internal const val InLibraryCoverAlpha = 0.34f

@Composable
fun SourceMangaBadges(
    inLibrary: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!inLibrary) return
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(10.dp))
            Text(stringResource(MR.strings.in_library), style = MaterialTheme.typography.labelSmall)
        }
    }
}
