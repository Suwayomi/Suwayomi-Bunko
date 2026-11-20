/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.MangaListItem
import dev.bunko.uicore.components.MangaListItemImage
import dev.bunko.uicore.components.MangaListItemTitle
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.components.rememberScrollbarAdapter
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SourceMangaList(
    mangas: ImmutableList<Manga>,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Manga) -> Unit,
    hasNextPage: Boolean,
    pageFailed: Boolean,
    onLoadNextPage: () -> Unit,
) {
    Box {
        val state = rememberLazyListState()
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.bottomNav.add(
                WindowInsets.navigationBars.only(
                    WindowInsetsSides.Bottom,
                ),
            ).asPaddingValues(),
        ) {
            items(mangas, key = { it.id }) { manga ->
                MangaListItem(
                    modifier = Modifier.combinedClickable(
                        onClick = { onClickManga(manga.id) },
                        onLongClick = { onLongClickManga(manga) },
                    ),
                    manga = manga,
                )
            }
            if (hasNextPage) {
                item(contentType = "footer") {
                    SourcePageFooter(mangas.size, pageFailed, onLoadNextPage)
                }
            }
        }
        VerticalScrollbar(
            rememberScrollbarAdapter(state),
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
private fun MangaListItem(
    modifier: Modifier,
    manga: Manga,
) {
    MangaListItem(
        modifier = modifier then Modifier
            .requiredHeight(64.dp)
            .padding(horizontal = 16.dp),
    ) {
        MangaListItemImage(
            modifier = Modifier
                .height(48.dp)
                .aspectRatio(mangaAspectRatio)
                .libraryCoverFrame()
                .alpha(if (manga.inLibrary) InLibraryCoverAlpha else 1f),
            data = manga,
            contentDescription = manga.title,
        )
        MangaListItemTitle(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            text = manga.title,
        )
        SourceMangaBadges(manga.inLibrary)
    }
}
