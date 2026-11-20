/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.tracking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bunko.domain.server.model.isAbsoluteHttpUrl
import dev.bunko.domain.server.model.serverResource
import dev.bunko.ui.base.image.ExternalImage
import dev.bunko.uicore.image.ImageLoaderImage
import io.ktor.http.Url

// image model: off-server icons load through the no-auth client so credentials never leave the server
fun trackerIconUrl(serverUrl: String, icon: String): Any? =
    runCatching {
        if (isAbsoluteHttpUrl(icon)) ExternalImage(Url(icon)) else Url(serverResource(serverUrl, icon))
    }.getOrNull()

@Composable
fun TrackerLogo(
    iconUrl: Any?,
    name: String,
    size: Dp = 48.dp,
) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size / 4)), contentAlignment = Alignment.Center) {
        if (iconUrl != null) {
            ImageLoaderImage(data = iconUrl, contentDescription = name, modifier = Modifier.size(size), onLoading = null)
        } else {
            Text(name.take(2), style = MaterialTheme.typography.titleMedium)
        }
    }
}
