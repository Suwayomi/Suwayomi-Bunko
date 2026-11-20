/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import dev.bunko.domain.ui.model.ThemeMode
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.ui.base.LocalViewModels
import dev.bunko.uicore.theme.ExtraColors
import dev.bunko.uicore.theme.Theme
import dev.bunko.uicore.theme.themes
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import me.tatarka.inject.annotations.Inject

private val DarkError = Color(0xFFF2B8B5)
private val LightError = Color(0xFFB3261E)

internal fun surfaceContainers(colors: Colors): Pair<Color, Color> {
    val (low, high) = if (colors.isLight) 0.05f to 0.09f else 0.08f to 0.11f
    return lerp(colors.surface, colors.onSurface, low) to lerp(colors.surface, colors.onSurface, high)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val viewModels = LocalViewModels.current
    val vm = remember { viewModels.appThemeViewModel() }
    val (colors, extraColors) = vm.getColors()
    DisposableEffect(vm) {
        onDispose(vm::onDispose)
    }

    val colorScheme = remember(colors, extraColors.surfaceContainer, extraColors.surfaceContainerHigh) {
        colors.toColorScheme(extraColors)
    }

    MaterialTheme(colors = colors) {
        androidx.compose.material3.MaterialTheme(colorScheme = colorScheme) {
            CompositionLocalProvider(
                androidx.compose.material.LocalContentColor provides colors.onBackground,
                androidx.compose.material3.LocalContentColor provides colorScheme.onBackground,
            ) {
                ExtraColors.WithExtraColors(extraColors, content)
            }
        }
    }
}

private fun Colors.toColorScheme(extra: ExtraColors): ColorScheme {
    val container = if (isLight) {
        lerp(Color.White, secondary, 0.2f)
    } else {
        lerp(Color.Black, secondary, 0.35f)
    }
    val onContainer = if (isLight) lerp(Color.Black, secondary, 0.3f) else lerp(Color.White, secondary, 0.15f)
    val onSurfaceVariant = onSurface.copy(alpha = 0.7f)
    val primaryContainer = if (isLight) lerp(Color.White, primary, 0.2f) else lerp(Color.Black, primary, 0.35f)
    val onPrimaryContainer = if (isLight) lerp(Color.Black, primary, 0.3f) else lerp(Color.White, primary, 0.15f)
    val outline = onSurface.copy(alpha = 0.5f)
    val outlineVariant = onSurface.copy(alpha = 0.2f)
    return if (isLight) {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = container,
            onSecondaryContainer = onContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = extra.surfaceContainer,
            onSurfaceVariant = onSurfaceVariant,
            error = error,
            onError = onError,
            surfaceContainerLowest = surface,
            surfaceContainerLow = extra.surfaceContainer,
            surfaceContainer = extra.surfaceContainer,
            surfaceContainerHigh = extra.surfaceContainerHigh,
            surfaceContainerHighest = extra.surfaceContainerHigh,
            surfaceBright = extra.surfaceContainerHigh,
            surfaceDim = surface,
        )
    } else {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = container,
            onSecondaryContainer = onContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = extra.surfaceContainer,
            onSurfaceVariant = onSurfaceVariant,
            error = error,
            onError = onError,
            surfaceContainerLowest = surface,
            surfaceContainerLow = extra.surfaceContainer,
            surfaceContainer = extra.surfaceContainer,
            surfaceContainerHigh = extra.surfaceContainerHigh,
            surfaceContainerHighest = extra.surfaceContainerHigh,
            surfaceBright = extra.surfaceContainerHigh,
            surfaceDim = surface,
        )
    }
}

@Inject
class AppThemeViewModel(
    private val uiPreferences: UiPreferences,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    override val scope = MainScope()

    private val themeMode = uiPreferences.themeMode().asStateFlow()
    private val lightTheme = uiPreferences.lightTheme().asStateFlow()
    private val darkTheme = uiPreferences.darkTheme().asStateFlow()
    private val pureBlack = uiPreferences.pureBlackDarkMode().asStateFlow()

    private val baseThemeJob = SupervisorJob()
    private val baseThemeScope = CoroutineScope(baseThemeJob)

    @Composable
    fun getColors(): Pair<Colors, ExtraColors> {
        val themeMode by themeMode.collectAsState()
        val lightTheme by lightTheme.collectAsState()
        val darkTheme by darkTheme.collectAsState()
        val pureBlack by pureBlack.collectAsState()

        val baseTheme = getBaseTheme(themeMode, lightTheme, darkTheme)
        val colors = remember(baseTheme.colors.isLight) {
            baseThemeJob.cancelChildren()

            if (baseTheme.colors.isLight) {
                uiPreferences.getLightColors().asStateFlow(baseThemeScope)
            } else {
                uiPreferences.getDarkColors().asStateFlow(baseThemeScope)
            }
        }

        val primary by colors.primaryStateFlow.collectAsState()
        val secondary by colors.secondaryStateFlow.collectAsState()
        val tertiary by colors.tertiaryStateFlow.collectAsState()

        val materialColors = getMaterialColors(baseTheme.colors, primary, secondary, pureBlack)
        return materialColors to getExtraColors(baseTheme.extraColors, tertiary, materialColors)
    }

    @Composable
    private fun getBaseTheme(
        themeMode: ThemeMode,
        lightTheme: Int,
        darkTheme: Int,
    ): Theme {
        fun getTheme(
            id: Int,
            isLight: Boolean,
        ): Theme =
            themes.find { it.id == id && it.colors.isLight == isLight }
                ?: themes.first { it.colors.isLight == isLight }

        return when (themeMode) {
            ThemeMode.System -> if (!isSystemInDarkTheme()) {
                getTheme(lightTheme, true)
            } else {
                getTheme(darkTheme, false)
            }

            ThemeMode.Light -> getTheme(lightTheme, true)

            ThemeMode.Dark -> getTheme(darkTheme, false)
        }
    }

    private fun getMaterialColors(
        baseColors: Colors,
        colorPrimary: Color,
        colorSecondary: Color,
        pureBlack: Boolean,
    ): Colors {
        val primary = colorPrimary.takeOrElse { baseColors.primary }
        val secondary = colorSecondary.takeOrElse { baseColors.secondary }
        val withAccents = baseColors.copy(
            primary = primary,
            primaryVariant = primary,
            secondary = secondary,
            secondaryVariant = secondary,
            onPrimary = if (primary.luminance() > 0.5) Color.Black else Color.White,
            onSecondary = if (secondary.luminance() > 0.5) Color.Black else Color.White,
        )
        return when {
            baseColors.isLight -> withAccents.copy(error = LightError, onError = Color.White)
            pureBlack -> withAccents.copy(
                background = Color.Black,
                surface = Color.Black,
                error = DarkError,
                onError = Color.Black,
            )
            else -> withAccents.copy(error = DarkError, onError = Color.Black)
        }
    }

    private fun getExtraColors(
        baseExtraColors: ExtraColors,
        colorTertiary: Color,
        colors: Colors,
    ): ExtraColors {
        val (container, containerHigh) = surfaceContainers(colors)
        return baseExtraColors.copy(
            tertiary = colorTertiary.takeOrElse { baseExtraColors.tertiary },
            surfaceContainer = container,
            surfaceContainerHigh = containerHigh,
        )
    }

    override fun onDispose() {
        baseThemeScope.cancel()
        scope.cancel()
    }
}
