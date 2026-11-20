/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.reader.SyncStatus
import dev.bunko.uicore.resources.stringResource

@Composable
fun SyncStatusChip(
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(status.queued > 0, modifier, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.bottomNav.add(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)))
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 3.dp,
        ) {
            Text(
                text = stringResource(
                    when {
                        status.authNeeded -> MR.strings.sync_auth_needed
                        status.offline -> MR.strings.sync_offline
                        else -> MR.strings.sync_waiting
                    },
                    status.queued,
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
