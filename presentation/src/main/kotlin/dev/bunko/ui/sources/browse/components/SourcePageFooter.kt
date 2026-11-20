/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource

/**
 * Sits after the last result while more pages exist. It asks for the next page whenever it comes into
 * view with a new item count, and offers a retry once a page failed so paging never silently stops.
 */
@Composable
fun SourcePageFooter(
    itemCount: Int,
    failed: Boolean,
    onLoadNextPage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (failed) {
            Text(
                stringResource(MR.strings.error_generic),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onLoadNextPage) {
                Text(stringResource(MR.strings.action_retry))
            }
        } else {
            LaunchedEffect(itemCount) { onLoadNextPage() }
            CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
        }
    }
}
