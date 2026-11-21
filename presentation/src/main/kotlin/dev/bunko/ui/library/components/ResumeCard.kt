/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import dev.bunko.uicore.theme.BunkoMotion

@Composable
fun ResumeCard(
    entry: ResumeEntry,
    expanded: Boolean,
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
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageLoaderImage(
                data = entry.manga,
                contentDescription = null,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp * mangaAspectRatio)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                onLoading = null,
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally(tween(BunkoMotion.LONG_MS, easing = BunkoMotion.EmphasizedDecelerate)) +
                    fadeIn(tween(BunkoMotion.MEDIUM_MS, delayMillis = BunkoMotion.SHORT_MS)),
                exit = shrinkHorizontally(tween(BunkoMotion.MEDIUM_MS, easing = BunkoMotion.EmphasizedAccelerate)) +
                    fadeOut(tween(BunkoMotion.SHORT_MS)),
            ) {
                Column(Modifier.padding(start = 12.dp).widthIn(max = 200.dp)) {
                    Text(
                        stringResource(MR.strings.continue_reading),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    Text(
                        entry.manga.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        chapterLabel,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(MR.strings.action_resume),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp).size(24.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}
