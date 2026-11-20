/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bunko.core.lang.launchIO
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.offline.OfflineChapterMeta
import dev.bunko.ui.base.offline.OfflineChapters
import dev.bunko.ui.base.offline.OfflineState
import dev.bunko.ui.library.components.libraryCoverFrame
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.reader.rememberReaderLauncher
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

class OfflineScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { offlineViewModel() }
        val readerLauncher = rememberReaderLauncher()
        OfflineScreenContent(
            series = vm.series.collectAsState().value,
            onOpen = { readerLauncher.launch(it.chapter.id, it.chapter.mangaId) },
            onDelete = vm::delete,
        )
    }
}

@Inject
class OfflineScreenViewModel(
    private val offlineChapters: OfflineChapters,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val series = offlineChapters.states
        .map { states ->
            val saved = states.filterValues { it == OfflineState.Downloaded }.keys
            withContext(Dispatchers.IO) {
                val store = offlineChapters.store
                store.all()
                    .filter { it.chapter.id in saved }
                    .groupByManga { store.coverPath(it)?.toFile() }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun delete(meta: OfflineChapterMeta) = scope.launchIO { offlineChapters.delete(listOf(meta.chapter)) }
}

@Composable
private fun OfflineScreenContent(
    series: List<OfflineManga>,
    onOpen: (OfflineChapterMeta) -> Unit,
    onDelete: (OfflineChapterMeta) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        ),
        topBar = { Toolbar(stringResource(MR.strings.location_offline)) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (series.isEmpty()) {
                Text(
                    stringResource(MR.strings.offline_empty),
                    Modifier.align(Alignment.Center).padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = WindowInsets.bottomNav
                        .add(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .asPaddingValues(),
                ) {
                    series.forEach { manga ->
                        val isExpanded = manga.id in expanded
                        item(key = "manga-${manga.id}") {
                            OfflineMangaRow(
                                manga = manga,
                                expanded = isExpanded,
                                onClick = { expanded = if (isExpanded) expanded - manga.id else expanded + manga.id },
                            )
                        }
                        if (isExpanded) {
                            items(manga.chapters, key = { it.chapter.id }) { meta ->
                                OfflineChapterRow(meta, onOpen = { onOpen(meta) }, onDelete = { onDelete(meta) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineMangaRow(
    manga: OfflineManga,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(56.dp)
                .aspectRatio(mangaAspectRatio)
                .libraryCoverFrame()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (manga.cover != null) {
                ImageLoaderImage(
                    manga.cover,
                    contentDescription = manga.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                manga.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(
                    if (manga.chapters.size == 1) MR.strings.manga_num_chapters_one else MR.strings.manga_num_chapters,
                    manga.chapters.size,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun OfflineChapterRow(
    meta: OfflineChapterMeta,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 88.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            meta.chapter.name,
            Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, stringResource(MR.strings.offline_remove))
        }
    }
}
