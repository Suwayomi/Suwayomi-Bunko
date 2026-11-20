/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.about.licenses.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource

private class Upstream(val name: String, val license: String, val url: String)

private val upstreams = listOf(
    Upstream("Suwayomi-JUI", "MPL-2.0", "https://github.com/Suwayomi/Suwayomi-JUI"),
    Upstream("Suwayomi-Server", "MPL-2.0", "https://github.com/Suwayomi/Suwayomi-Server"),
    Upstream("Suwayomi-Tsumiru", "MPL-2.0", "https://github.com/Suwayomi/Suwayomi-Tsumiru"),
    Upstream("Komikku", "Apache-2.0", "https://github.com/komikku-app/komikku"),
    Upstream("Mihon", "Apache-2.0", "https://github.com/mihonapp/mihon"),
    Upstream("Tachiyomi", "Apache-2.0", "https://github.com/tachiyomiorg/tachiyomi"),
)

@Composable
fun UpstreamCredits() {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))) {
        Text(
            stringResource(MR.strings.upstream_credits),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        upstreams.forEach {
            Column(Modifier.fillMaxWidth().clickable { uriHandler.openUri(it.url) }.padding(vertical = 8.dp)) {
                Text(it.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    it.license,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
