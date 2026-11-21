/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.theme

import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.bunko.core.prefs.Preference
import dev.bunko.uicore.theme.DYNAMIC_THEME_ID
import dev.bunko.uicore.theme.atTone
import dev.bunko.uicore.theme.chroma
import dev.bunko.uicore.theme.onColor
import dev.bunko.uicore.theme.tone
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
import androidx.compose.ui.graphics.takeOrElse
import dev.bunko.domain.ui.model.ThemeMode
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.ui.base.LocalViewModels
import dev.bunko.uicore.theme.ExtraColors
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

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
val dynamicThemeSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Immutable
data class AppColors(
    val colors: Colors,
    val extraColors: ExtraColors,
    val scheme: ColorScheme,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val viewModels = LocalViewModels.current
    val vm = remember { viewModels.appThemeViewModel() }
    val (colors, extraColors, colorScheme) = vm.getColors()
    DisposableEffect(vm) {
        onDispose(vm::onDispose)
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

private fun presetScheme(
    colors: Colors,
    tertiary: Color,
): ColorScheme {
    val dark = !colors.isLight
    fun container(c: Color) = c.atTone(if (dark) 30.0 else 90.0)
    fun onContainer(c: Color) = c.atTone(if (dark) 90.0 else 10.0)

    val background = colors.background
    val bgTone = background.tone()
    val neutral = if (background.chroma() < 3.0) colors.primary else background
    fun surfaceAt(offset: Double) = neutral.atTone((bgTone + offset).coerceIn(0.0, 100.0), 10.0)
    val step = if (dark) 1.0 else -1.0
    val lowest = if (dark) background else Color.White
    val onSurface = colors.onSurface

    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = container(colors.primary),
        onPrimaryContainer = onContainer(colors.primary),
        inversePrimary = colors.primary.atTone(if (dark) 40.0 else 80.0),
        secondary = colors.secondary,
        onSecondary = colors.onSecondary,
        secondaryContainer = container(colors.secondary),
        onSecondaryContainer = onContainer(colors.secondary),
        tertiary = tertiary,
        onTertiary = tertiary.onColor(),
        tertiaryContainer = container(tertiary),
        onTertiaryContainer = onContainer(tertiary),
        background = background,
        onBackground = colors.onBackground,
        surface = colors.surface,
        onSurface = onSurface,
        surfaceVariant = surfaceAt(step * 13),
        onSurfaceVariant = neutral.atTone(if (dark) 80.0 else 30.0, 8.0),
        surfaceTint = colors.primary,
        inverseSurface = neutral.atTone(if (dark) 90.0 else 20.0, 6.0),
        inverseOnSurface = neutral.atTone(if (dark) 20.0 else 95.0, 6.0),
        error = colors.error,
        onError = colors.onError,
        errorContainer = container(colors.error),
        onErrorContainer = onContainer(colors.error),
        outline = neutral.atTone(if (dark) 60.0 else 50.0, 8.0),
        outlineVariant = neutral.atTone(if (dark) 30.0 else 80.0, 8.0),
        scrim = Color.Black,
        surfaceBright = surfaceAt(step * 18),
        surfaceDim = surfaceAt(if (dark) 0.0 else -11.0),
        surfaceContainerLowest = lowest,
        surfaceContainerLow = surfaceAt(step * 3),
        surfaceContainer = surfaceAt(step * 5),
        surfaceContainerHigh = surfaceAt(step * 9),
        surfaceContainerHighest = surfaceAt(step * 13),
    )
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

    init {
        dropUnknownThemes()
    }

    @Composable
    fun getColors(): AppColors {
        val themeMode by themeMode.collectAsState()
        val lightTheme by lightTheme.collectAsState()
        val darkTheme by darkTheme.collectAsState()
        val pureBlack by pureBlack.collectAsState()

        val isLight = when (themeMode) {
            ThemeMode.System -> !isSystemInDarkTheme()
            ThemeMode.Light -> true
            ThemeMode.Dark -> false
        }
        val themeId = if (isLight) lightTheme else darkTheme
        if (themeId == DYNAMIC_THEME_ID && dynamicThemeSupported) {
            val context = LocalContext.current
            val configuration = LocalConfiguration.current
            return remember(isLight, pureBlack, configuration) { dynamicAppColors(context, isLight, pureBlack) }
        }

        val baseTheme = themes.find { it.id == themeId && it.colors.isLight == isLight }
            ?: themes.first { it.colors.isLight == isLight }
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
        val tertiaryColor = tertiary.takeOrElse { baseTheme.extraColors.tertiary }
        val scheme = remember(materialColors, tertiaryColor) { presetScheme(materialColors, tertiaryColor) }
        val extraColors = remember(scheme) {
            baseTheme.extraColors.copy(
                tertiary = scheme.tertiary,
                onTertiary = scheme.onTertiary,
                surfaceContainer = scheme.surfaceContainer,
                surfaceContainerHigh = scheme.surfaceContainerHigh,
            )
        }
        return AppColors(materialColors, extraColors, scheme)
    }

    private fun dropUnknownThemes() {
        fun repair(
            id: Preference<Int>,
            isLight: Boolean,
            accents: AppColorsPreference,
        ) {
            val value = id.get()
            val known = value == 0 ||
                (value == DYNAMIC_THEME_ID && dynamicThemeSupported) ||
                themes.any { it.id == value && it.colors.isLight == isLight }
            if (known) return
            id.delete()
            accents.primary.delete()
            accents.secondary.delete()
            accents.tertiary.delete()
        }
        repair(uiPreferences.lightTheme(), true, uiPreferences.getLightColors())
        repair(uiPreferences.darkTheme(), false, uiPreferences.getDarkColors())
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
            onPrimary = primary.onColor(),
            onSecondary = secondary.onColor(),
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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun dynamicAppColors(
        context: Context,
        isLight: Boolean,
        pureBlack: Boolean,
    ): AppColors {
        val system = if (isLight) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        val scheme = if (!isLight && pureBlack) {
            system.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceContainerLowest = Color.Black,
            )
        } else {
            system
        }
        val colors = if (isLight) {
            lightColors(
                primary = scheme.primary,
                primaryVariant = scheme.primaryContainer,
                secondary = scheme.secondary,
                secondaryVariant = scheme.secondaryContainer,
                background = scheme.background,
                surface = scheme.surface,
                error = scheme.error,
                onPrimary = scheme.onPrimary,
                onSecondary = scheme.onSecondary,
                onBackground = scheme.onBackground,
                onSurface = scheme.onSurface,
                onError = scheme.onError,
            )
        } else {
            darkColors(
                primary = scheme.primary,
                primaryVariant = scheme.primaryContainer,
                secondary = scheme.secondary,
                secondaryVariant = scheme.secondaryContainer,
                background = scheme.background,
                surface = scheme.surface,
                error = scheme.error,
                onPrimary = scheme.onPrimary,
                onSecondary = scheme.onSecondary,
                onBackground = scheme.onBackground,
                onSurface = scheme.onSurface,
                onError = scheme.onError,
            )
        }
        val extraColors = ExtraColors(
            tertiary = scheme.tertiary,
            onTertiary = scheme.onTertiary,
            surfaceContainer = scheme.surfaceContainer,
            surfaceContainerHigh = scheme.surfaceContainerHigh,
        )
        return AppColors(colors, extraColors, scheme)
    }

    override fun onDispose() {
        baseThemeScope.cancel()
        scope.cancel()
    }
}
