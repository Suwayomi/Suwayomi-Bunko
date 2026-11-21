/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.uicore.components.MangaListItem
import dev.bunko.uicore.components.MangaListItemImage
import dev.bunko.uicore.components.MangaListItemTitle
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.components.selectedBackground
import kotlinx.collections.immutable.ImmutableList

@Composable
fun LibraryMangaList(
    library: ImmutableList<Manga>,
    selection: Set<Long>,
    onClickManga: (Long) -> Unit,
    onLongClickManga: (Long) -> Unit,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
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
            items(library, key = { it.id }, contentType = { "manga" }) { manga ->
                LibraryMangaListItem(
                    modifier = Modifier.animateItem().libraryMangaClickable(
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
private fun LibraryMangaListItem(
    modifier: Modifier,
    manga: Manga,
    selected: Boolean,
    showUnread: Boolean,
    showDownloaded: Boolean,
    showLanguage: Boolean,
    showLocal: Boolean,
) {
    MangaListItem(
        modifier = modifier then Modifier
            .selectedBackground(selected)
            .requiredHeight(64.dp)
            .padding(horizontal = 16.dp),
    ) {
        MangaListItemImage(
            modifier = Modifier
                .height(48.dp)
                .aspectRatio(mangaAspectRatio)
                .libraryCoverFrame(selected),
            data = manga,
            contentDescription = manga.title,
        )
        MangaListItemTitle(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            text = manga.title,
        )
        Box(Modifier.width(IntrinsicSize.Min)) {
            LibraryMangaBadges(
                manga = manga,
                showUnread = showUnread,
                showDownloaded = showDownloaded,
                showLanguage = showLanguage,
                showLocal = showLocal,
            )
        }
    }
}
