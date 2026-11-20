/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.source.model.Source
import dev.bunko.i18n.MR
import dev.bunko.ui.base.ListSectionHeader
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.sources.components.SourceStatusChip
import dev.bunko.ui.sources.home.SourceUI
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.components.LoadingScreen
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SourceHomeScreenContent(
    onSourceClick: (Source) -> Unit,
    onLatestClick: (Source) -> Unit,
    onTogglePinned: (Source) -> Unit,
    isLoading: Boolean,
    sources: ImmutableList<SourceUI>,
) {
    if (isLoading && sources.isEmpty()) {
        LoadingScreen()
    } else if (sources.isEmpty()) {
        EmptyScreen(stringResource(MR.strings.sources_empty), icon = Icons.Outlined.Explore)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.bottomNav.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
            ).asPaddingValues(),
        ) {
            sources.forEach { sourceUI ->
                when (sourceUI) {
                    is SourceUI.Header -> stickyHeader(key = "header-${sourceUI.header}", contentType = "header") {
                        ListSectionHeader(sourceUI.header)
                    }

                    is SourceUI.SourceItem -> item(key = sourceUI.source.id, contentType = "source") {
                        SourceItem(
                            sourceUI,
                            onSourceClick = onSourceClick,
                            onLatestClick = onLatestClick,
                            onTogglePinned = onTogglePinned,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceItem(
    sourceItem: SourceUI.SourceItem,
    onSourceClick: (Source) -> Unit,
    onLatestClick: (Source) -> Unit,
    onTogglePinned: (Source) -> Unit,
) {
    val source = sourceItem.source
    Row(
        Modifier.fillMaxWidth()
            .clickable { onSourceClick(source) }
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageLoaderImage(
            data = source,
            contentDescription = source.displayName,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
            filterQuality = FilterQuality.Medium,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                source.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (source.lang != Source.LOCAL_SOURCE_LANG) SourceStatusChip(source.lang.uppercase())
                if (source.isNsfw) {
                    SourceStatusChip("18+", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (source.supportsLatest) {
            TextButton(onClick = { onLatestClick(source) }) {
                Text(stringResource(MR.strings.move_to_latest))
            }
        }
        IconButton(onClick = { onTogglePinned(source) }) {
            val pinned = sourceItem.isPinned
            Icon(
                if (pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                contentDescription = stringResource(if (pinned) MR.strings.action_unpin else MR.strings.action_pin),
                tint = if (pinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
