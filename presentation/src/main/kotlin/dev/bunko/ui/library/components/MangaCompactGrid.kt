/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.components.rememberVerticalScrollbarAdapter
import dev.bunko.uicore.image.ImageLoaderImage
import kotlinx.collections.immutable.ImmutableList

@Composable
fun LibraryMangaCompactGrid(
    library: ImmutableList<Manga>,
    gridColumns: Int,
    gridSize: Int,
    selection: Set<Long>,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Long) -> Unit,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
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
            contentPadding = libraryGridContentPadding(),
            verticalArrangement = Arrangement.spacedBy(LibraryGridSpacing),
            horizontalArrangement = Arrangement.spacedBy(LibraryGridSpacing),
        ) {
            items(library, key = { it.id }) { manga ->
                LibraryMangaCompactGridItem(
                    modifier = Modifier.libraryMangaClickable(
                        { onClickManga(manga.id) },
                        { onLongClickManga(manga.id) },
                    ),
                    manga = manga,
                    selected = manga.id in selection,
                    showUnread = showUnread,
                    showDownloaded = showDownloaded,
                    showLanguage = showLanguage,
                    showLocal = showLocal,
                )
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
private fun LibraryMangaCompactGridItem(
    modifier: Modifier,
    manga: Manga,
    selected: Boolean,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(mangaAspectRatio)
            .libraryCoverFrame(selected) then modifier,
    ) {
        ImageLoaderImage(
            manga,
            manga.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
        )
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xAA000000),
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
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black,
                    blurRadius = 4f,
                ),
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        LibraryMangaBadges(
            modifier = Modifier.padding(4.dp),
            manga = manga,
            showUnread = showUnread,
            showDownloaded = showDownloaded,
            showLanguage = showLanguage,
            showLocal = showLocal,
        )
    }
}
