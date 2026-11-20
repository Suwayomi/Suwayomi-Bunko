/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.base.offline.OfflineState
import dev.bunko.uicore.resources.stringResource

@Composable
fun OfflineChapterButton(
    state: OfflineState?,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            when (state) {
                null -> onSave()
                is OfflineState.Downloading -> onRemove()
                OfflineState.Downloaded -> menu = true
            }
        },
    ) {
        when (state) {
            null -> Icon(
                Icons.Outlined.PhoneAndroid,
                stringResource(MR.strings.offline_save),
                Modifier.size(22.dp),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )

            is OfflineState.Downloading -> CircularProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
            )

            OfflineState.Downloaded -> Icon(
                Icons.Rounded.PhoneAndroid,
                stringResource(MR.strings.offline_on_device),
                Modifier.size(22.dp),
                MaterialTheme.colorScheme.tertiary,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.offline_remove)) },
                onClick = {
                    menu = false
                    onRemove()
                },
            )
        }
    }
}
