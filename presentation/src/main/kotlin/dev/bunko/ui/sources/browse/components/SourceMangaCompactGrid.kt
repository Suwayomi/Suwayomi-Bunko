/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.components.rememberVerticalScrollbarAdapter
import dev.bunko.uicore.image.ImageLoaderImage
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SourceMangaCompactGrid(
    mangas: ImmutableList<Manga>,
    gridColumns: Int,
    gridSize: Int,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Manga) -> Unit,
    hasNextPage: Boolean,
    pageFailed: Boolean,
    onLoadNextPage: () -> Unit,
    showTitles: Boolean = true,
) {
    Box {
        val state = rememberLazyGridState()
        val cells = if (gridColumns < 1) {
            GridCells.Adaptive(gridSize.dp)
        } else {
            GridCells.Fixed(gridColumns)
        }
        LazyVerticalGrid(
            columns = cells,
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.bottomNav.add(
                WindowInsets.navigationBars.only(
                    WindowInsetsSides.Bottom,
                ),
            ).add(WindowInsets(left = 8.dp, top = 8.dp, right = 8.dp, bottom = 8.dp)).asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(mangas, key = { it.id }) { manga ->
                SourceMangaCompactGridItem(
                    modifier = Modifier.combinedClickable(
                        onClick = { onClickManga(manga.id) },
                        onLongClick = { onLongClickManga(manga) },
                    ),
                    manga = manga,
                    showTitle = showTitles,
                )
            }
            if (hasNextPage) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                    SourcePageFooter(mangas.size, pageFailed, onLoadNextPage)
                }
            }
        }
        VerticalScrollbar(
            rememberVerticalScrollbarAdapter(state, cells),
            Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight()
                .windowInsetsPadding(
                    WindowInsets.bottomNav.add(
                        WindowInsets.navigationBars.only(
                            WindowInsetsSides.Bottom,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun SourceMangaCompactGridItem(
    modifier: Modifier,
    manga: Manga,
    showTitle: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(mangaAspectRatio)
            .libraryCoverFrame() then modifier,
    ) {
        ImageLoaderImage(
            manga,
            manga.title,
            modifier = Modifier.fillMaxSize().alpha(if (manga.inLibrary) InLibraryCoverAlpha else 1f),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
        )
        if (showTitle) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.67f),
                        ),
                    )
                    .fillMaxHeight(0.33f)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )
            Text(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                text = manga.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black,
                        blurRadius = 4f,
                    ),
                ),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SourceMangaBadges(
            inLibrary = manga.inLibrary,
            modifier = Modifier.padding(4.dp),
        )
    }
}
