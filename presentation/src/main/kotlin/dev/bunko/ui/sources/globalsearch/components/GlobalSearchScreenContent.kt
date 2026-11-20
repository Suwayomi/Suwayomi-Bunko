/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.globalsearch.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.source.model.Source
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.sources.browse.components.InLibraryCoverAlpha
import dev.bunko.ui.sources.browse.components.SourceMangaBadges
import dev.bunko.ui.sources.globalsearch.GlobalSearchViewModel.Search
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList

private val CoverCardWidth = 112.dp

@Composable
fun GlobalSearchScreenContent(
    sources: ImmutableList<Source>,
    results: SnapshotStateMap<Long, Search>,
    query: String,
    setQuery: (String) -> Unit,
    submitSearch: (String) -> Unit,
    onSourceClick: (Source) -> Unit,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(
                name = stringResource(MR.strings.location_global_search),
                searchText = query,
                search = setQuery,
                searchSubmit = { submitSearch(query) },
            )
        },
    ) { padding ->
        val ordered by remember(sources) {
            derivedStateOf {
                sources.filter { results[it.id] is Search.Success } +
                    sources.filter { results[it.id] == null } +
                    sources.filter { results[it.id] is Search.Failure }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = WindowInsets.bottomNav.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
            ).asPaddingValues(),
        ) {
            items(ordered, key = { it.id }) {
                GlobalSearchItem(
                    source = it,
                    search = results[it.id] ?: Search.Searching,
                    onSourceClick = onSourceClick,
                    onMangaClick = onMangaClick,
                    onMangaLongClick = onMangaLongClick,
                )
            }
        }
    }
}

@Composable
fun GlobalSearchItem(
    source: Source,
    search: Search,
    onSourceClick: (Source) -> Unit,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clickable { onSourceClick(source) }
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageLoaderImage(
                data = source,
                contentDescription = source.displayName,
                modifier = Modifier.size(32.dp).clip(MaterialTheme.shapes.small),
                filterQuality = FilterQuality.Medium,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    source.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (source.lang != Source.LOCAL_SOURCE_LANG) {
                    Text(
                        source.displayLang,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                stringResource(MR.strings.action_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (search) {
            is Search.Failure -> Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (search.message == MR.strings.no_results_found) Icons.Rounded.SearchOff else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(search.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Search.Searching -> Box(
                Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }

            is Search.Success -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(search.mangaList, key = { it.id }) { manga ->
                    GlobalSearchMangaItem(
                        manga,
                        onClick = { onMangaClick(manga) },
                        onLongClick = { onMangaLongClick(manga) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalSearchMangaItem(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        Modifier.width(CoverCardWidth)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
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
            SourceMangaBadges(
                inLibrary = manga.inLibrary,
                modifier = Modifier.padding(4.dp),
            )
        }
        Text(
            text = manga.title,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
