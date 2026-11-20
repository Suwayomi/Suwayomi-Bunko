/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bunko.uicore.components.mangaAspectRatio

@Immutable
data class ThemePreviewColors(
    val background: Color,
    val onBackground: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

@Composable
fun ThemePreviewCard(
    colors: ThemePreviewColors,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(modifier.width(112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(CardShape)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .border(if (selected) 3.dp else 1.dp, ring, CardShape)
                .padding(if (selected) 6.dp else 4.dp)
                .clip(InnerShape)
                .background(colors.background),
        ) {
            MiniScreen(colors)
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.background, modifier = Modifier.size(16.dp))
                }
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MiniScreen(colors: ThemePreviewColors) {
    val muted = colors.onBackground.copy(alpha = 0.12f).compositeOver(colors.background)
    val strong = colors.onBackground.copy(alpha = 0.7f).compositeOver(colors.background)
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.padding(start = 8.dp, top = 12.dp, end = 34.dp).fillMaxWidth().height(8.dp)
                .background(strong, RoundedCornerShape(4.dp)),
        )
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(2) { index ->
                Box(Modifier.weight(1f).aspectRatio(mangaAspectRatio).background(muted, RoundedCornerShape(6.dp))) {
                    Box(
                        Modifier.padding(4.dp).size(width = 14.dp, height = 8.dp)
                            .background(if (index == 0) colors.tertiary else colors.secondary, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().background(muted).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 22.dp, height = 12.dp).background(colors.primary, CircleShape))
            repeat(2) { Box(Modifier.size(10.dp).background(strong.copy(alpha = 0.5f), CircleShape)) }
        }
    }
}

private val CardShape = RoundedCornerShape(16.dp)
private val InnerShape = RoundedCornerShape(12.dp)
