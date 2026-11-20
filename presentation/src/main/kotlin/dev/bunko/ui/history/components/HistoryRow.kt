/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.history.HistoryEntry
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.uicore.components.MangaListItemImage
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.resources.stringResource
import java.util.Date

@Composable
fun HistoryRow(
    entry: HistoryEntry,
    onResume: () -> Unit,
    onOpenManga: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onResume)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val coverModifier = Modifier.height(64.dp).aspectRatio(mangaAspectRatio).libraryCoverFrame()
        if (entry.cover != null) {
            MangaListItemImage(
                modifier = coverModifier.clickable(onClick = onOpenManga),
                data = entry.cover,
                contentDescription = entry.mangaTitle,
            )
        } else {
            Box(coverModifier)
        }
        Column(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = entry.mangaTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.chapter.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = progressLine(entry),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(MR.strings.history_remove))
            }
        }
    }
}

@Composable
private fun progressLine(entry: HistoryEntry): String {
    val context = LocalContext.current
    val time = entry.readAt?.let { seconds ->
        remember(seconds) { android.text.format.DateFormat.getTimeFormat(context).format(Date(seconds * 1000)) }
    }
    val progress = if (entry.chapter.read) {
        stringResource(MR.strings.history_read)
    } else {
        stringResource(MR.strings.history_page, entry.chapter.lastPageRead + 1)
    }
    return listOfNotNull(time, progress).joinToString(" · ")
}
