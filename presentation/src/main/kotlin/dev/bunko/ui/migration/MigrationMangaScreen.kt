/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.migration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.bunko.domain.manga.model.Manga
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.screen.BaseScreen
import dev.bunko.ui.base.userMessage
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadStateCrossfade
import dev.bunko.uicore.components.SkeletonList
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource

class MigrationMangaScreen(
    private val sourceId: Long,
    private val sourceName: String?,
) : BaseScreen() {
    @Composable
    override fun Content() {
        val vm = viewModel { migrationLibraryViewModel() }
        val navigator = LocalNavigator.currentOrThrow
        val state = vm.state.collectAsState().value
        LaunchedEffect(vm) { vm.start() }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.statusBars.add(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
            ),
            topBar = { Toolbar(sourceName ?: stringResource(MR.strings.migrate_unknown_source, sourceId.toString())) },
        ) { padding ->
            LoadStateCrossfade(state, Modifier.padding(padding)) { current ->
                when (current) {
                    MigrationLibraryState.Loading -> SkeletonList(coverHeight = 64.dp, lines = 1)

                    is MigrationLibraryState.Failed -> ErrorScreen(stringResource(current.error.userMessage), retry = vm::load)

                    is MigrationLibraryState.Loaded -> {
                        val manga = current.sources.find { it.sourceId == sourceId }?.manga.orEmpty()
                        if (manga.isEmpty()) {
                            EmptyScreen(stringResource(MR.strings.migrate_empty), icon = Icons.Rounded.SwapHoriz)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
                            ) {
                                items(manga, key = { it.id }, contentType = { "manga" }) {
                                    MigrationMangaItem(it, onClick = { navigator push MigrationSearchScreen(it.id, it.title) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MigrationMangaItem(
    manga: Manga,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageLoaderImage(
            manga,
            contentDescription = manga.title,
            modifier = Modifier.height(64.dp).aspectRatio(mangaAspectRatio).clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            manga.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
