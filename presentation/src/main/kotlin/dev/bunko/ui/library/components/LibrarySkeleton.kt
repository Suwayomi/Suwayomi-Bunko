/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.domain.library.model.DisplayMode
import dev.bunko.uicore.components.SkeletonBlock
import dev.bunko.uicore.components.SkeletonHost
import dev.bunko.uicore.components.SkeletonList
import dev.bunko.uicore.components.SkeletonTextLine
import dev.bunko.uicore.components.mangaAspectRatio

private val SkeletonCoverShape = RoundedCornerShape(8.dp)
private const val SkeletonGridItems = 30

@Composable
fun LibrarySkeleton(
    displayMode: DisplayMode,
    gridColumns: Int,
    gridSize: Int,
    contentPadding: PaddingValues = libraryGridContentPadding(),
) {
    if (displayMode == DisplayMode.List) {
        SkeletonList(coverHeight = 48.dp, lines = 1)
        return
    }
    val withTitle = displayMode == DisplayMode.ComfortableGrid
    SkeletonHost(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = if (gridColumns < 1) GridCells.Adaptive(gridSize.dp) else GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(LibraryGridSpacing),
            horizontalArrangement = Arrangement.spacedBy(LibraryGridSpacing),
        ) {
            items(SkeletonGridItems, contentType = { "skeleton" }) {
                Column {
                    SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(mangaAspectRatio), SkeletonCoverShape)
                    if (withTitle) {
                        Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SkeletonTextLine(fraction = 0.9f)
                            SkeletonTextLine(fraction = 0.55f)
                        }
                    }
                }
            }
        }
    }
}
