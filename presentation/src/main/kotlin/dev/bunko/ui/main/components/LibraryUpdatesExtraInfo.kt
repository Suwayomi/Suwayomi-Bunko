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
import dev.bunko.i18n.MR
import dev.bunko.ui.base.LocalViewModels
import dev.bunko.uicore.resources.stringResource

@Composable
fun LibraryUpdatesExtraInfo() {
    val viewModels = LocalViewModels.current
    val vm = remember { viewModels.libraryUpdatesViewModel(true) }
    DisposableEffect(vm) {
        onDispose(vm::onDispose)
    }
    val serviceStatus by vm.serviceStatus.collectAsState()
    val jobsInfo by vm.jobsInfo.collectAsState()

    val text = when (serviceStatus) {
        WebsocketService.Status.STARTING -> stringResource(MR.strings.downloads_loading)

        WebsocketService.Status.RUNNING -> {
            if (jobsInfo.isRunning) {
                stringResource(MR.strings.notification_updating, jobsInfo.finishedJobs, jobsInfo.totalJobs)
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
                .clickable(onClick = vm::restartLibraryUpdates),
        ) {
            Text(
                stringResource(MR.strings.downloads_stopped),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
