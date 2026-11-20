/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import dev.bunko.domain.base.WebsocketService
import dev.bunko.domain.download.model.DownloaderState
import dev.bunko.i18n.MR
import dev.bunko.ui.base.LocalViewModels
import dev.bunko.uicore.resources.stringResource

@Composable
fun DownloadsExtraInfo() {
    val viewModels = LocalViewModels.current
    val vm = remember { viewModels.downloadsViewModel(true) }
    DisposableEffect(vm) {
        onDispose(vm::onDispose)
    }
    val serviceStatus by vm.serviceStatus.collectAsState()
    val downloaderStatus by vm.downloaderStatus.collectAsState()
    val list by vm.downloadQueue.collectAsState()
    val text = when (serviceStatus) {
        WebsocketService.Status.STARTING -> stringResource(MR.strings.downloads_loading)

        WebsocketService.Status.RUNNING -> {
            if (list.isNotEmpty()) {
                val remainingDownloads = stringResource(MR.strings.downloads_remaining, list.size)
                if (downloaderStatus == DownloaderState.STOPPED) {
                    stringResource(MR.strings.downloads_paused) + "  •  " + remainingDownloads
                } else {
                    remainingDownloads
                }
            } else {
                null
            }
        }

        WebsocketService.Status.STOPPED -> null
    }
    if (!text.isNullOrBlank()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (serviceStatus == WebsocketService.Status.STOPPED) {
        Box(
            Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = vm::restartDownloader),
        ) {
            Text(
                stringResource(MR.strings.downloads_stopped),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
