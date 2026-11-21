/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import dev.bunko.uicore.theme.BunkoMotion

private const val TAB_FADE_IN_DELAY_MS = 30
private val SlideDistance = 30.dp

@Composable
fun BunkoScreenTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val slide = remember(density) { with(density) { SlideDistance.roundToPx() } }
    AnimatedContent(
        targetState = navigator.lastItem,
        transitionSpec = {
            when (navigator.lastEvent) {
                StackEvent.Push -> sharedAxisX(forward = true, slide)
                StackEvent.Pop -> sharedAxisX(forward = false, slide)
                else -> fadeThrough()
            }
        },
        contentKey = { it.key },
        modifier = modifier,
        label = "screen",
    ) { screen ->
        navigator.saveableState("transition", screen) {
            screen.Content()
        }
    }
}

private fun sharedAxisX(
    forward: Boolean,
    slide: Int,
): ContentTransform {
    val motion = tween<IntOffset>(BunkoMotion.LONG_MS, easing = BunkoMotion.EmphasizedDecelerate)
    val enter = slideInHorizontally(motion) { if (forward) slide else -slide } +
        fadeIn(
            tween(
                BunkoMotion.LONG_MS - BunkoMotion.SHORT_MS,
                delayMillis = BunkoMotion.SHORT_MS,
                easing = LinearOutSlowInEasing,
            ),
        )
    val exit = slideOutHorizontally(motion) { if (forward) -slide else slide } +
        fadeOut(tween(BunkoMotion.SHORT_MS, easing = FastOutLinearInEasing))
    return enter togetherWith exit
}

private fun fadeThrough(): ContentTransform =
    fadeIn(tween(BunkoMotion.MEDIUM_MS, delayMillis = TAB_FADE_IN_DELAY_MS, easing = BunkoMotion.EmphasizedDecelerate)) togetherWith
        fadeOut(tween(BunkoMotion.SHORT_MS + TAB_FADE_IN_DELAY_MS, easing = FastOutLinearInEasing))
