/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.components

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.manga.model.MangaStatus
import dev.bunko.i18n.MR
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.dialog.MultiChoiceList
import kotlinx.collections.immutable.toImmutableList

private const val SecondaryAlpha = 0.78f

private const val DisabledAlpha = 0.38f

@Composable
fun MangaItem(
    manga: Manga,
    categoriesVisible: Boolean,
    favoriteEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    onEditCategories: () -> Unit,
    onOpenInBrowser: (() -> Unit)?,
    trackedCount: Int,
    onOpenTracking: () -> Unit,
    onCoverBitmap: (Bitmap) -> Unit,
    topInset: Dp = 0.dp,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        MangaInfoBox(manga, onCoverBitmap, topInset)
        MangaActionRow(
            inLibrary = manga.inLibrary,
            favoriteEnabled = favoriteEnabled,
            categoriesVisible = categoriesVisible,
            onToggleFavorite = onToggleFavorite,
            onEditCategories = onEditCategories,
            onOpenInBrowser = onOpenInBrowser,
            trackedCount = trackedCount,
            onOpenTracking = onOpenTracking,
        )
        Synopsis(manga, expanded) { expanded = !expanded }
    }
}

@Composable
private fun MangaInfoBox(
    manga: Manga,
    onCoverBitmap: (Bitmap) -> Unit,
    topInset: Dp = 0.dp,
) {
    Box(Modifier.fillMaxWidth()) {
        val background = MaterialTheme.colorScheme.background
        val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, background),
                            startY = size.height / 2,
                        ),
                    )
                }
                .background(tint)
                .blur(7.dp)
                .alpha(0.2f),
        ) {
            ImageLoaderImage(
                data = manga,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = null,
                onFailure = null,
                onBitmap = onCoverBitmap,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = topInset + 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(manga, Modifier.width(100.dp).libraryCoverFrame())
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MangaContentInfo(manga)
            }
        }
    }
}

@Composable
private fun Cover(
    manga: Manga,
    modifier: Modifier = Modifier,
) {
    ImageLoaderImage(
        data = manga,
        contentDescription = manga.title,
        modifier = modifier,
        errorModifier = modifier then Modifier
            .aspectRatio(
                ratio = mangaAspectRatio,
                matchHeightConstraintsFirst = true,
            ),
        filterQuality = FilterQuality.Medium,
    )
}

@Composable
private fun MangaContentInfo(manga: Manga) {
    Text(
        text = manga.title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(2.dp))

    manga.author?.takeIf { it.isNotBlank() }?.let {
        InfoRow(Icons.Filled.PersonOutline, it)
    }
    manga.artist?.takeIf { it.isNotBlank() && it != manga.author }?.let {
        InfoRow(Icons.Filled.Brush, it)
    }

    Spacer(Modifier.height(2.dp))
    val meta = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = manga.status.icon(),
            contentDescription = null,
            tint = meta,
            modifier = Modifier.padding(end = 4.dp).size(16.dp),
        )
        Text(
            text = listOfNotNull(stringResource(manga.status.res), manga.source?.displayName).joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            color = meta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
) {
    val meta = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = meta, modifier = Modifier.size(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = meta,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun MangaStatus.icon(): ImageVector =
    when (this) {
        MangaStatus.ONGOING -> Icons.Outlined.Schedule
        MangaStatus.COMPLETED -> Icons.Outlined.DoneAll
        MangaStatus.LICENSED -> Icons.Outlined.AttachMoney
        MangaStatus.PUBLISHING_FINISHED -> Icons.Outlined.Done
        MangaStatus.CANCELLED -> Icons.Outlined.Close
        MangaStatus.ON_HIATUS -> Icons.Outlined.Pause
        MangaStatus.UNKNOWN -> Icons.Outlined.Block
    }

@Composable
private fun MangaActionRow(
    inLibrary: Boolean,
    favoriteEnabled: Boolean,
    categoriesVisible: Boolean,
    onToggleFavorite: () -> Unit,
    onEditCategories: () -> Unit,
    onOpenInBrowser: (() -> Unit)?,
    trackedCount: Int,
    onOpenTracking: () -> Unit,
) {
    val defaultColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
    Row(Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
        MangaActionButton(
            title = stringResource(if (inLibrary) MR.strings.in_library else MR.strings.add_to_library),
            icon = if (inLibrary) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            color = if (inLibrary) MaterialTheme.colorScheme.primary else defaultColor,
            enabled = favoriteEnabled,
            onClick = onToggleFavorite,
        )
        if (categoriesVisible) {
            MangaActionButton(
                title = stringResource(MR.strings.edit_categories),
                icon = Icons.AutoMirrored.Outlined.Label,
                color = defaultColor,
                onClick = onEditCategories,
            )
        }
        MangaActionButton(
            title = if (trackedCount == 0) {
                stringResource(MR.strings.tracking_action)
            } else {
                stringResource(MR.strings.tracking_action_count, trackedCount)
            },
            icon = if (trackedCount == 0) Icons.Outlined.Sync else Icons.Filled.Done,
            color = if (trackedCount == 0) defaultColor else MaterialTheme.colorScheme.primary,
            onClick = onOpenTracking,
        )
        if (onOpenInBrowser != null) {
            MangaActionButton(
                title = stringResource(MR.strings.action_open_in_browser),
                icon = Icons.Outlined.Public,
                color = MaterialTheme.colorScheme.primary,
                onClick = onOpenInBrowser,
            )
        }
    }
}

@Composable
private fun RowScope.MangaActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = color,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Synopsis(
    manga: Manga,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val hasDescription = !manga.description.isNullOrEmpty()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).animateContentSize()) {
        if (hasDescription) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = manga.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (manga.genre.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            if (expanded) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    manga.genre.fastForEach { Chip(it) }
                }
            } else {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    manga.genre.fastForEach { Chip(it) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Chip(text: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(text, maxLines = 1) },
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
fun ChapterHeader(
    shownCount: Int,
    totalCount: Int,
    missingCount: Int,
    customised: Boolean,
    onOpenSettings: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onOpenSettings)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = when {
                    shownCount != totalCount -> stringResource(MR.strings.manga_num_chapters_filtered, shownCount, totalCount)
                    totalCount == 1 -> stringResource(MR.strings.manga_num_chapters_one, totalCount)
                    else -> stringResource(MR.strings.manga_num_chapters, totalCount)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (missingCount > 0) {
                Text(
                    text = stringResource(MR.strings.missing_chapters, missingCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = SecondaryAlpha),
                    maxLines = 1,
                )
            }
        }
        IconButton(onClick = onOpenSettings, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = stringResource(MR.strings.action_filter),
                tint = if (customised) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = SecondaryAlpha)
                },
            )
        }
    }
}

@Composable
fun MissingChaptersRow(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text = stringResource(MR.strings.missing_chapters, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
fun CategorySelectDialog(
    state: DialogState,
    categories: ImmutableList<Category>,
    oldCategories: ImmutableList<Category>,
    onPositiveClick: (List<Category>, List<Category>) -> Unit,
) {
    if (!state.isShowing) return
    var checked by remember { mutableStateOf(oldCategories.map { it.id }.toSet()) }
    AppDialog(
        state = state,
        title = stringResource(MR.strings.select_categories),
        confirm = DialogButton(stringResource(MR.strings.action_ok)) {
            onPositiveClick(categories.filter { it.id in checked }, oldCategories)
        },
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        MultiChoiceList(
            categories.map { it.id to it.name }.toImmutableList(),
            checked,
        ) { checked = if (it in checked) checked - it else checked + it }
    }
}
