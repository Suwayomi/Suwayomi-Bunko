/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bunko.ui.library.components.LibraryCoverShape
import dev.bunko.uicore.components.SkeletonBlock
import dev.bunko.uicore.components.SkeletonHost
import dev.bunko.uicore.components.SkeletonTextLine
import dev.bunko.uicore.components.mangaAspectRatio

private const val SkeletonChapterRows = 8

@Composable
fun MangaScreenSkeleton(topInset: Dp) {
    SkeletonHost(Modifier.fillMaxSize()) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, top = topInset + 8.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(Modifier.width(100.dp).aspectRatio(mangaAspectRatio), LibraryCoverShape)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonTextLine(fraction = 0.9f, height = 20.dp)
                    SkeletonTextLine(fraction = 0.6f, height = 20.dp)
                    SkeletonTextLine(fraction = 0.45f)
                    SkeletonTextLine(fraction = 0.35f)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(4) { SkeletonBlock(Modifier.size(40.dp), CircleShape) }
            }
            Column(
                Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonTextLine()
                SkeletonTextLine()
                SkeletonTextLine(fraction = 0.7f)
            }
            ChapterRowsSkeleton(Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
fun ChapterListSkeleton() {
    SkeletonHost(Modifier.fillMaxWidth()) {
        ChapterRowsSkeleton()
    }
}

@Composable
private fun ChapterRowsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        repeat(SkeletonChapterRows) {
            Column(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonTextLine(fraction = 0.6f, height = 14.dp)
                SkeletonTextLine(fraction = 0.3f, height = 10.dp)
            }
        }
    }
}
