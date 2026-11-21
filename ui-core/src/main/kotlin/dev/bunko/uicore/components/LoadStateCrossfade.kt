/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.uicore.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.bunko.uicore.theme.BunkoMotion

@Composable
fun <S : Any> LoadStateCrossfade(
    state: S,
    modifier: Modifier = Modifier,
    contentKey: (S) -> Any = { it::class },
    content: @Composable (S) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(BunkoMotion.MEDIUM_MS, easing = LinearOutSlowInEasing)) togetherWith
                fadeOut(tween(BunkoMotion.SHORT_MS)) using null
        },
        contentKey = contentKey,
        label = "load-state",
    ) { content(it) }
}
