/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import dev.bunko.ui.base.userMessage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.domain.category.interactor.GetCategories
import dev.bunko.domain.library.model.DisplayMode
import dev.bunko.domain.library.service.LibraryPreferences
import dev.bunko.i18n.MR
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.ChoicePreference
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.ui.base.prefs.SwitchPreference
import dev.bunko.ui.categories.CategoriesScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.prefs.PreferenceMutableStateFlow
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import kotlin.math.roundToInt
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogContentPadding
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.dialog.rememberDialogState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme

class SettingsLibraryScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { settingsLibraryViewModel() }
        val navigator = LocalNavigator.currentOrThrow
        SettingsLibraryScreenContent(
            showAllCategory = vm.showAllCategory,
            displayMode = vm.displayMode,
            displayModeChoices = vm.getDisplayModeChoices(),
            gridColumns = vm.gridColumns,
            gridColumnsLandscape = vm.gridColumnsLandscape,
            gridSize = vm.gridSize,
            unreadBadge = vm.unreadBadge,
            downloadBadge = vm.downloadBadge,
            languageBadge = vm.languageBadge,
            localBadge = vm.localBadge,
            showContinueReading = vm.showContinueReading,
            categoriesSize = vm.categories.collectAsState().value,
            openCategoriesScreen = { navigator.push(CategoriesScreen()) },
        )
    }
}

@Inject
class SettingsLibraryViewModel(
    libraryPreferences: LibraryPreferences,
    getCategories: GetCategories,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val displayMode = libraryPreferences.displayMode().asStateFlow()
    val gridColumns = libraryPreferences.gridColumns().asStateFlow()
    val gridColumnsLandscape = libraryPreferences.gridColumnsLandscape().asStateFlow()
    val unreadBadge = libraryPreferences.unreadBadge().asStateFlow()
    val downloadBadge = libraryPreferences.downloadBadge().asStateFlow()
    val languageBadge = libraryPreferences.languageBadge().asStateFlow()
    val localBadge = libraryPreferences.localBadge().asStateFlow()
    val showContinueReading = libraryPreferences.showContinueReading().asStateFlow()
    val gridSize = libraryPreferences.gridSize().asStateFlow()

    val showAllCategory = libraryPreferences.showAllCategory().asStateFlow()
    val categories = getCategories.asFlow(true)
        .map { it.size }
        .catch { toast(it.userMessage.toPlatformString()) }
        .stateIn(scope, SharingStarted.Eagerly, 0)


    @Composable
    fun getDisplayModeChoices() =
        DisplayMode.entries
            .associateWith { stringResource(it.res) }
            .toImmutableMap()
}

@Composable
fun SettingsLibraryScreenContent(
    displayMode: PreferenceMutableStateFlow<DisplayMode>,
    displayModeChoices: ImmutableMap<DisplayMode, String>,
    gridColumns: PreferenceMutableStateFlow<Int>,
    gridColumnsLandscape: PreferenceMutableStateFlow<Int>,
    gridSize: PreferenceMutableStateFlow<Int>,
    unreadBadge: PreferenceMutableStateFlow<Boolean>,
    downloadBadge: PreferenceMutableStateFlow<Boolean>,
    languageBadge: PreferenceMutableStateFlow<Boolean>,
    localBadge: PreferenceMutableStateFlow<Boolean>,
    showContinueReading: PreferenceMutableStateFlow<Boolean>,
    showAllCategory: PreferenceMutableStateFlow<Boolean>,
    categoriesSize: Int,
    openCategoriesScreen: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.settings_library_screen))
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
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_display))
                }
                item {
                    ChoicePreference(
                        preference = displayMode,
                        choices = displayModeChoices,
                        title = stringResource(MR.strings.display_mode),
                    )
                }
                item {
                    val displayModePref by displayMode.collectAsState()
                    GridPreference(
                        columnPreference = gridColumns,
                        sizePreference = gridSize,
                        title = stringResource(MR.strings.items_per_row_portrait),
                        enabled = displayModePref != DisplayMode.List,
                    )
                }
                item {
                    val displayModePref by displayMode.collectAsState()
                    GridPreference(
                        columnPreference = gridColumnsLandscape,
                        sizePreference = gridSize,
                        title = stringResource(MR.strings.items_per_row_landscape),
                        enabled = displayModePref != DisplayMode.List,
                    )
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_badges))
                }
                item {
                    SwitchPreference(unreadBadge, stringResource(MR.strings.display_badge_unread))
                }
                item {
                    SwitchPreference(downloadBadge, stringResource(MR.strings.display_badge_downloaded))
                }
                item {
                    SwitchPreference(languageBadge, stringResource(MR.strings.display_badge_language))
                }
                item {
                    SwitchPreference(localBadge, stringResource(MR.strings.display_badge_local))
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.other))
                }
                item {
                    SwitchPreference(showContinueReading,stringResource(MR.strings.display_continue_reading))
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_organization))
                }
                item {
                    PreferenceRow(
                        stringResource(MR.strings.location_categories),
                        onClick = { openCategoriesScreen() },
                        subtitle = categoriesSize.toString(),
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

@Composable
private fun GridPreference(
    columnPreference: PreferenceMutableStateFlow<Int>,
    sizePreference: PreferenceMutableStateFlow<Int>,
    title: String,
    enabled: Boolean,
) {
    val columnPrefValue by columnPreference.collectAsState()
    val sizePrefValue by sizePreference.collectAsState()
    val dialogState = rememberDialogState()
    PreferenceRow(
        title = title,
        subtitle = if (columnPrefValue < 1) {
            stringResource(MR.strings.items_per_row_sub_adaptive, sizePrefValue)
        } else {
            stringResource(MR.strings.items_per_row_sub, columnPrefValue)
        },
        onClick = {
            dialogState.show()
        },
        enabled = enabled,
    )
    GridPrefDialog(
        state = dialogState,
        initialColumns = columnPrefValue,
        initialSize = sizePrefValue,
        title = title,
        onSelected = { columns, size ->
            columnPreference.value = columns
            sizePreference.value = size
        },
    )
}

@Composable
private fun GridPrefDialog(
    state: DialogState,
    initialColumns: Int,
    initialSize: Int,
    onCloseRequest: () -> Unit = {},
    onSelected: (columns: Int, size: Int) -> Unit,
    title: String,
) {
    var columns by remember(initialColumns) { mutableStateOf(initialColumns.toFloat()) }
    var size by remember(initialSize) { mutableStateOf(initialSize.toFloat()) }
    AppDialog(
        state = state,
        title = title,
        confirm = DialogButton(stringResource(MR.strings.action_ok)) {
            onSelected(columns.roundToInt(), roundedSize(size))
        },
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onCloseRequest,
    ) {
        Column(DialogContentPadding) {
            val columnsInt = columns.roundToInt()
            val adaptive = columnsInt < 1
            SliderLabel(
                stringResource(MR.strings.grid_columns),
                if (adaptive) stringResource(MR.strings.adaptive) else columnsInt.toString(),
            )
            Slider(
                value = columns,
                onValueChange = { columns = it },
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0F..10F,
                steps = 10 - 2,
            )
            AnimatedVisibility(adaptive) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    SliderLabel(stringResource(MR.strings.grid_size), roundedSize(size).toString())
                    Slider(
                        value = size,
                        onValueChange = { size = it },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = 90F..300F,
                        steps = 21 - 2,
                    )
                }
            }
        }
    }
}

internal fun roundedSize(size: Float): Int = (size / 10f).roundToInt() * 10

@Composable
private fun SliderLabel(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}
