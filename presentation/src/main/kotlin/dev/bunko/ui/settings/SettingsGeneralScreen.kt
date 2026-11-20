/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import dev.bunko.core.lang.getDisplayName
import dev.bunko.core.lang.withIOContext
import dev.bunko.core.util.DateHandler
import dev.bunko.domain.ui.model.StartScreen
import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.ChoicePreference
import dev.bunko.ui.base.prefs.SwitchPreference
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.prefs.PreferenceMutableStateFlow
import dev.bunko.uicore.resources.readTextAsync
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

class SettingsGeneralScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { settingsGeneralViewModel() }
        SettingsGeneralScreenContent(
            startScreen = vm.startScreen,
            startScreenChoices = vm.getStartScreenChoices(),
            confirmExit = vm.confirmExit,
            language = vm.language,
            languageChoices = vm.getLanguageChoices(),
            dateFormat = vm.dateFormat,
            dateFormatChoices = vm.getDateChoices(),
        )
    }
}

@Inject
class SettingsGeneralViewModel(
    private val dateHandler: DateHandler,
    uiPreferences: UiPreferences,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val startScreen = uiPreferences.startScreen().asStateFlow()
    val confirmExit = uiPreferences.confirmExit().asStateFlow()
    val language = uiPreferences.language().asStateFlow()
    val dateFormat = uiPreferences.dateFormat().asStateFlow()

    private val now = Clock.System.now()
    private val currentLocale = Locale.current

    @Composable
    fun getStartScreenChoices(): ImmutableMap<StartScreen, String> =
        persistentMapOf(
            StartScreen.Library to stringResource(MR.strings.location_library),
            StartScreen.History to stringResource(MR.strings.location_history),
            StartScreen.Schedule to stringResource(MR.strings.location_schedule),
            StartScreen.Sources to stringResource(MR.strings.location_sources),
            StartScreen.Extensions to stringResource(MR.strings.location_extensions),
        )

    @Composable
    fun getLanguageChoices(): ImmutableMap<String, String> {
        val langJsonState = MR.files.languages_json.readTextAsync()
        val langs by produceState(emptyMap(), langJsonState.value) {
            val langJson = langJsonState.value
            if (langJson != null) {
                withIOContext {
                    value = Json.decodeFromString<JsonObject>(langJson)["langs"]
                        ?.jsonArray
                        .orEmpty()
                        .map { it.jsonPrimitive.content }
                        .associateWith { Locale(it).getDisplayName(currentLocale) }
                }
            }
        }
        return mapOf(
            "" to stringResource(
                MR.strings.language_system_default,
                currentLocale.getDisplayName(currentLocale),
            ),
        )
            .plus(langs)
            .toImmutableMap()
    }

    @Composable
    fun getDateChoices(): ImmutableMap<String, String> =
        dateHandler.formatOptions
            .associateWith {
                it.ifEmpty { stringResource(MR.strings.date_system_default) } +
                    " (${getFormattedDate(it)})"
            }
            .toImmutableMap()

    @Composable
    private fun getFormattedDate(prefValue: String): String = dateHandler.getDateFormat(prefValue).invoke(now)
}

@Composable
fun SettingsGeneralScreenContent(
    startScreen: PreferenceMutableStateFlow<StartScreen>,
    startScreenChoices: ImmutableMap<StartScreen, String>,
    confirmExit: PreferenceMutableStateFlow<Boolean>,
    language: PreferenceMutableStateFlow<String>,
    languageChoices: ImmutableMap<String, String>,
    dateFormat: PreferenceMutableStateFlow<String>,
    dateFormatChoices: ImmutableMap<String, String>,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.settings_general_screen))
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
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_behavior))
                }
                item {
                    ChoicePreference(
                        preference = startScreen,
                        title = stringResource(MR.strings.start_screen),
                        choices = startScreenChoices,
                    )
                }
                item {
                    SwitchPreference(
                        preference = confirmExit,
                        title = stringResource(MR.strings.confirm_exit),
                    )
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_localization))
                }
                item {
                    ChoicePreference(
                        preference = language,
                        title = stringResource(MR.strings.language),
                        choices = languageChoices,
                    )
                }
                item {
                    ChoicePreference(
                        preference = dateFormat,
                        title = stringResource(MR.strings.date_format),
                        choices = dateFormatChoices,
                    )
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
