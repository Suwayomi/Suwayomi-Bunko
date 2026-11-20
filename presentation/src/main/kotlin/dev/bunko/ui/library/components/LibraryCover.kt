/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.library.components

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bunko.ui.main.components.bottomNav

private val LibraryCoverShape = RoundedCornerShape(8.dp)

val LibraryGridPadding: Dp = 12.dp
val LibraryGridSpacing: Dp = 8.dp

@Composable
fun libraryGridContentPadding(): PaddingValues {
    val insets = WindowInsets.bottomNav
        .add(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        .asPaddingValues()
    return PaddingValues(
        start = LibraryGridPadding,
        top = LibraryGridPadding,
        end = LibraryGridPadding,
        bottom = insets.calculateBottomPadding() + LibraryGridPadding,
    )
}

@Composable
fun Modifier.libraryCoverFrame(selected: Boolean = false): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    return clip(LibraryCoverShape)
        .drawWithContent {
            drawContent()
            if (selected) drawRect(primary.copy(alpha = 0.3f))
        }.border(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) primary else outline.copy(alpha = 0.5f),
            shape = LibraryCoverShape,
        )
}

fun Modifier.libraryMangaClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = combinedClickable(onClick = onClick, onLongClick = onLongClick)
