/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.ui.library.components.libraryMangaClickable
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.components.rememberVerticalScrollbarAdapter
import dev.bunko.uicore.image.ImageLoaderImage
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SourceMangaComfortableGrid(
    mangas: ImmutableList<Manga>,
    gridColumns: Int,
    gridSize: Int,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Manga) -> Unit,
    hasNextPage: Boolean,
    pageFailed: Boolean,
    onLoadNextPage: () -> Unit,
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
            items(mangas, key = { it.id }, contentType = { "manga" }) { manga ->
                SourceMangaComfortableGridItem(
                    modifier = Modifier.animateItem().libraryMangaClickable(
                        { onClickManga(manga.id) },
                        { onLongClickManga(manga) },
                        clipShape = MaterialTheme.shapes.medium,
                    ),
                    manga = manga,
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
private fun SourceMangaComfortableGridItem(
    modifier: Modifier,
    manga: Manga,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth() then modifier,
    ) {
        Column {
            ImageLoaderImage(
                manga,
                contentDescription = manga.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(mangaAspectRatio)
                    .libraryCoverFrame()
                    .alpha(if (manga.inLibrary) InLibraryCoverAlpha else 1f),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
            )
            Text(
                text = manga.title,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        SourceMangaBadges(
            inLibrary = manga.inLibrary,
            modifier = Modifier.padding(4.dp),
        )
    }
}
