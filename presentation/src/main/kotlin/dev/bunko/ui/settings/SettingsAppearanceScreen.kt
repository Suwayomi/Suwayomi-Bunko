/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Colors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bunko.domain.ui.model.ThemeMode
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.ColorPreference
import dev.bunko.ui.base.prefs.SwitchPreference
import dev.bunko.ui.base.theme.AppColorsPreferenceState
import dev.bunko.ui.base.theme.asStateFlow
import dev.bunko.ui.base.theme.getDarkColors
import dev.bunko.ui.base.theme.getLightColors
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.prefs.PreferenceMutableStateFlow
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.theme.ExtraColors
import dev.bunko.uicore.theme.extraColors
import dev.bunko.uicore.theme.themes
import dev.bunko.uicore.theme.DYNAMIC_THEME_ID
import dev.bunko.ui.base.theme.dynamicThemeSupported
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import me.tatarka.inject.annotations.Inject
import androidx.compose.material.MaterialTheme as M2Theme

class SettingsAppearanceScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { themesViewModel() }
        val appThemeVM = viewModel { appThemeViewModel() }
        val (colors, extraColors) = appThemeVM.getColors()
        SettingsAppearanceScreenContent(
            activeColors = vm.getActiveColors(),
            themeMode = vm.themeMode,
            lightTheme = vm.lightTheme,
            darkTheme = vm.darkTheme,
            pureBlack = vm.pureBlack,
            customColors = colors,
            customExtraColors = extraColors,
        )
    }
}

@Inject
class ThemesViewModel(
    private val uiPreferences: UiPreferences,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val themeMode = uiPreferences.themeMode().asStateFlow()
    val lightTheme = uiPreferences.lightTheme().asStateFlow()
    val darkTheme = uiPreferences.darkTheme().asStateFlow()
    val pureBlack = uiPreferences.pureBlackDarkMode().asStateFlow()
    val lightColors = uiPreferences.getLightColors().asStateFlow(scope)
    val darkColors = uiPreferences.getDarkColors().asStateFlow(scope)

    @Composable
    fun getActiveColors(): AppColorsPreferenceState = if (M2Theme.colors.isLight) lightColors else darkColors
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceScreenContent(
    activeColors: AppColorsPreferenceState,
    themeMode: PreferenceMutableStateFlow<ThemeMode>,
    lightTheme: PreferenceMutableStateFlow<Int>,
    darkTheme: PreferenceMutableStateFlow<Int>,
    pureBlack: PreferenceMutableStateFlow<Boolean>,
    customColors: Colors,
    customExtraColors: ExtraColors,
) {
    val isLight = M2Theme.colors.isLight
    val themesForCurrentMode = remember(isLight) {
        themes.filter { it.colors.isLight == isLight }
    }
    val currentLightTheme by lightTheme.collectAsState()
    val currentDarkTheme by darkTheme.collectAsState()
    val currentMode by themeMode.collectAsState()
    val pureBlackOn by pureBlack.collectAsState()
    val dynamicSelected = dynamicThemeSupported &&
        (if (isLight) currentLightTheme else currentDarkTheme) == DYNAMIC_THEME_ID

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.settings_appearance_screen))
        },
    ) {
        Box(Modifier.padding(it)) {
            val state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = WindowInsets.bottomNav.add(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Bottom,
                    ),
                ).asPaddingValues(),
            ) {
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.theme))
                    val modes = listOf(
                        ThemeMode.System to stringResource(MR.strings.theme_system),
                        ThemeMode.Light to stringResource(MR.strings.theme_light),
                        ThemeMode.Dark to stringResource(MR.strings.theme_dark),
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        modes.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = currentMode == mode,
                                onClick = { themeMode.value = mode },
                                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            ) { Text(label) }
                        }
                    }
                }
                if (!isLight) {
                    item {
                        SwitchPreference(
                            preference = pureBlack,
                            title = stringResource(MR.strings.theme_pure_black),
                            subtitle = stringResource(MR.strings.theme_pure_black_sub),
                        )
                    }
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.preset_themes))
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (dynamicThemeSupported) {
                            item(key = DYNAMIC_THEME_ID) {
                                val context = LocalContext.current
                                val system = remember(isLight, LocalConfiguration.current) {
                                    if (isLight) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
                                }
                                ThemePreviewCard(
                                    colors = ThemePreviewColors(
                                        background = if (pureBlackOn && !isLight) Color.Black else system.background,
                                        onBackground = system.onBackground,
                                        primary = system.primary,
                                        secondary = system.secondary,
                                        tertiary = system.tertiary,
                                    ),
                                    title = stringResource(MR.strings.theme_dynamic),
                                    selected = dynamicSelected,
                                    onClick = {
                                        (if (isLight) lightTheme else darkTheme).value = DYNAMIC_THEME_ID
                                        activeColors.primaryStateFlow.value = Color.Unspecified
                                        activeColors.secondaryStateFlow.value = Color.Unspecified
                                        activeColors.tertiaryStateFlow.value = Color.Unspecified
                                    },
                                )
                            }
                        }
                        items(themesForCurrentMode, key = { it.id }) { theme ->
                            val isSelected = theme.id == if (isLight) currentLightTheme else currentDarkTheme
                            ThemePreviewCard(
                                colors = previewColors(
                                    if (isSelected) customColors else theme.colors,
                                    if (isSelected) customExtraColors else theme.extraColors,
                                    pureBlack = pureBlackOn,
                                ),
                                title = stringResource(theme.titleRes),
                                selected = isSelected,
                                onClick = {
                                    (if (isLight) lightTheme else darkTheme).value = theme.id
                                    activeColors.primaryStateFlow.value = theme.colors.primary
                                    activeColors.secondaryStateFlow.value = theme.colors.secondary
                                    activeColors.tertiaryStateFlow.value = theme.extraColors.tertiary
                                },
                            )
                        }
                    }
                }
                if (dynamicSelected) {
                    item {
                        PreferenceSectionHeader(stringResource(MR.strings.theme_colors))
                        Text(
                            stringResource(MR.strings.theme_dynamic_colors_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    item {
                        PreferenceSectionHeader(stringResource(MR.strings.theme_colors))
                    }
                    item {
                        ColorPreference(
                            preference = activeColors.primaryStateFlow,
                            title = stringResource(MR.strings.color_primary),
                            subtitle = stringResource(MR.strings.color_primary_sub),
                            unsetColor = M2Theme.colors.primary,
                        )
                    }
                    item {
                        ColorPreference(
                            preference = activeColors.secondaryStateFlow,
                            title = stringResource(MR.strings.color_secondary),
                            subtitle = stringResource(MR.strings.color_secondary_sub),
                            unsetColor = M2Theme.colors.secondary,
                        )
                    }
                    item {
                        ColorPreference(
                            preference = activeColors.tertiaryStateFlow,
                            title = stringResource(MR.strings.color_tertiary),
                            subtitle = stringResource(MR.strings.color_secondary_sub),
                            unsetColor = M2Theme.extraColors.tertiary,
                        )
                    }
                }
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(state),
                Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .windowInsetsPadding(
                        WindowInsets.bottomNav.add(
                            WindowInsets.navigationBars.only(
                                WindowInsetsSides.Bottom,
                            ),
                        ),
                    ),
            )
        }
    }
}

private fun previewColors(
    colors: Colors,
    extraColors: ExtraColors,
    pureBlack: Boolean,
) = ThemePreviewColors(
    background = if (pureBlack && !colors.isLight) Color.Black else colors.background,
    onBackground = colors.onBackground,
    primary = colors.primary,
    secondary = colors.secondary,
    tertiary = extraColors.tertiary,
)
