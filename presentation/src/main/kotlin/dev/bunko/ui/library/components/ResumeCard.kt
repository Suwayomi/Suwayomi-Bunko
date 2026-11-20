/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.library.ResumeEntry
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource

@Composable
fun ResumeCard(
    entry: ResumeEntry,
    onResume: (chapterId: Long, mangaId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chapterLabel = if (entry.inProgress) {
        stringResource(MR.strings.continue_reading_page, entry.chapter.name, entry.chapter.lastPageRead + 1)
    } else {
        entry.chapter.name
    }
    Surface(
        onClick = { onResume(entry.chapter.id, entry.manga.id) },
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageLoaderImage(
                data = entry.manga,
                contentDescription = null,
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp * mangaAspectRatio)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                onLoading = null,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(MR.strings.continue_reading),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    entry.manga.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    chapterLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(MR.strings.action_resume),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
