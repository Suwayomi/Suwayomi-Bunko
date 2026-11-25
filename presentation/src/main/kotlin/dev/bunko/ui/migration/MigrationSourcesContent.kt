/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.migration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.source.model.Source
import dev.bunko.i18n.MR
import dev.bunko.ui.base.userMessage
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadStateCrossfade
import dev.bunko.uicore.components.SkeletonList
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource

@Composable
fun MigrationSourcesContent(
    state: MigrationLibraryState,
    start: () -> Unit,
    retry: () -> Unit,
    onSourceClick: (MigrationSource) -> Unit,
) {
    LaunchedEffect(Unit) { start() }
    LoadStateCrossfade(state, contentKey = { it::class }) { current ->
        when (current) {
            MigrationLibraryState.Loading ->
                SkeletonList(coverHeight = 44.dp, lines = 2, coverAspectRatio = 1f, coverShape = RoundedCornerShape(10.dp))

            is MigrationLibraryState.Failed -> ErrorScreen(stringResource(current.error.userMessage), retry = retry)

            is MigrationLibraryState.Loaded -> if (current.sources.isEmpty()) {
                EmptyScreen(stringResource(MR.strings.migrate_empty), icon = Icons.Rounded.SwapHoriz)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = WindowInsets.bottomNav.add(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                    ).asPaddingValues(),
                ) {
                    items(current.sources, key = { it.sourceId }, contentType = { "source" }) {
                        MigrationSourceItem(it, onClick = { onSourceClick(it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MigrationSourceItem(
    item: MigrationSource,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val source = item.source
        if (source != null) {
            ImageLoaderImage(
                data = source,
                contentDescription = source.displayName,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                filterQuality = FilterQuality.Medium,
            )
        } else {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                source?.name ?: stringResource(MR.strings.migrate_unknown_source, item.sourceId.toString()),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    source?.displayLang?.takeIf { source.lang != Source.LOCAL_SOURCE_LANG },
                    stringResource(MR.strings.migrate_source_entries, item.manga.size),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
