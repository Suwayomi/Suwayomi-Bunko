/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.uicore.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DropdownIconButton(
    key: Any? = Unit,
    dropdownItems: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var showMenu by remember(key) { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center, content = content)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            dropdownItems { showMenu = false }
        }
    }
}
