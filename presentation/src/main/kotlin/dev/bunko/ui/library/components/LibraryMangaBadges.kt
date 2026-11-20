/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.source.model.Source
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource

@Composable
fun LibraryMangaBadges(
    modifier: Modifier = Modifier,
    manga: Manga,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
) {
    val unread = manga.unreadCount?.takeIf { showUnread && it > 0 }
    val downloaded = manga.downloadCount?.takeIf { showDownloaded && it > 0 }
    val isLocal = (manga.sourceId == Source.LOCAL_SOURCE_ID).takeIf { showLocal } ?: false
    val language = manga.source?.lang?.takeIf { showLanguage }?.toUpperCase(Locale.current)

    Row(modifier then Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (unread != null || downloaded != null) {
            Row(Modifier.clip(BadgeShape)) {
                if (downloaded != null) {
                    BadgeSegment(
                        text = downloaded.toString(),
                        color = MaterialTheme.colorScheme.tertiary,
                        textColor = MaterialTheme.colorScheme.onTertiary,
                    )
                }
                if (unread != null) {
                    BadgeSegment(
                        text = unread.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        } else {
            Spacer(Modifier)
        }

        val endText = if (isLocal) stringResource(MR.strings.local_badge) else language
        if (endText != null) {
            Row(Modifier.clip(BadgeShape)) {
                BadgeSegment(
                    text = endText,
                    color = MaterialTheme.colorScheme.secondary,
                    textColor = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }
    }
}

private val BadgeShape = RoundedCornerShape(4.dp)

@Composable
private fun BadgeSegment(
    text: String,
    color: Color,
    textColor: Color,
) {
    Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .background(color)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}
