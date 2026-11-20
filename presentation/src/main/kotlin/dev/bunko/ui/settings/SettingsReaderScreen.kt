/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import dev.bunko.domain.reader.model.Direction
import dev.bunko.domain.reader.model.ImageSaveTarget
import dev.bunko.domain.reader.model.ImageScale
import dev.bunko.domain.reader.model.NavigationMode
import dev.bunko.domain.reader.model.ReaderBackground
import dev.bunko.domain.reader.model.TappingInvertMode
import dev.bunko.domain.reader.service.ReaderModePreferences
import dev.bunko.domain.reader.service.ReaderPreferences
import dev.bunko.i18n.MR
import dev.bunko.ui.base.model.StableHolder
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.ChoicePreference
import dev.bunko.ui.base.prefs.SwitchPreference
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.reader.readerModeLabel
import dev.bunko.ui.reader.readerModeResource
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.prefs.PreferenceMutableStateFlow
import dev.bunko.uicore.prefs.asStateIn
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

class SettingsReaderScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { settingsReaderViewModel() }
        SettingsReaderScreenContent(
            modes = vm.modes.collectAsState().value,
            selectedMode = vm.selectedMode,
            modeSettings = vm.modeSettings.collectAsState().value,
            directionChoices = vm.getDirectionChoices(),
            paddingChoices = vm.getPaddingChoices(),
            getMaxSizeChoices = vm::getMaxSizeChoices,
            imageScaleChoices = vm.getImageScaleChoices(),
            navigationModeChoices = vm.getNavigationModeChoices(),
            general = vm.general,
        )
    }
}

data class ReaderGeneralPreferences(
    val background: PreferenceMutableStateFlow<ReaderBackground>,
    val backgroundChoices: ImmutableMap<ReaderBackground, String>,
    val keepScreenOn: PreferenceMutableStateFlow<Boolean>,
    val volumeKeys: PreferenceMutableStateFlow<Boolean>,
    val volumeKeysInverted: PreferenceMutableStateFlow<Boolean>,
    val tappingInvertMode: PreferenceMutableStateFlow<TappingInvertMode>,
    val tappingInvertChoices: ImmutableMap<TappingInvertMode, String>,
    val imageSaveTarget: PreferenceMutableStateFlow<ImageSaveTarget>,
    val imageSaveTargetChoices: ImmutableMap<ImageSaveTarget, String>,
    val imageSaveFolder: PreferenceMutableStateFlow<String>,
)

@Inject
class SettingsReaderViewModel(
    readerPreferences: ReaderPreferences,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val modes = readerPreferences.modes().asStateFlow()
        .map {
            it.associateWith { mode -> readerModeResource(mode)?.toPlatformString() ?: mode }
                .toImmutableMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())
    val selectedMode = readerPreferences.mode().asStateIn(scope)

    val general = ReaderGeneralPreferences(
        background = readerPreferences.background().asStateIn(scope),
        backgroundChoices = mapOf(
            ReaderBackground.Black to MR.strings.reader_background_black,
            ReaderBackground.Gray to MR.strings.reader_background_gray,
            ReaderBackground.White to MR.strings.reader_background_white,
            ReaderBackground.System to MR.strings.reader_background_system,
        ).mapValues { it.value.toPlatformString() }.toImmutableMap(),
        keepScreenOn = readerPreferences.keepScreenOn().asStateIn(scope),
        volumeKeys = readerPreferences.volumeKeys().asStateIn(scope),
        volumeKeysInverted = readerPreferences.volumeKeysInverted().asStateIn(scope),
        tappingInvertMode = readerPreferences.tappingInvertMode().asStateIn(scope),
        tappingInvertChoices = mapOf(
            TappingInvertMode.NONE to MR.strings.tap_invert_none,
            TappingInvertMode.HORIZONTAL to MR.strings.tap_invert_horizontal,
            TappingInvertMode.VERTICAL to MR.strings.tap_invert_vertical,
            TappingInvertMode.BOTH to MR.strings.tap_invert_both,
        ).mapValues { it.value.toPlatformString() }.toImmutableMap(),
        imageSaveTarget = readerPreferences.imageSaveTarget().asStateIn(scope),
        imageSaveTargetChoices = mapOf(
            ImageSaveTarget.Ask to MR.strings.image_save_target_ask,
            ImageSaveTarget.Phone to MR.strings.image_save_target_phone,
            ImageSaveTarget.Server to MR.strings.image_save_target_server,
        ).mapValues { it.value.toPlatformString() }.toImmutableMap(),
        imageSaveFolder = readerPreferences.imageSaveFolder().asStateIn(scope),
    )

    private val _modeSettings = MutableStateFlow<ImmutableList<StableHolder<ReaderModePreference>>>(
        persistentListOf(),
    )
    val modeSettings = _modeSettings.asStateFlow()

    init {
        modes.onEach { modes ->
            val modeSettings = _modeSettings.value
            val modesInSettings = modeSettings.map { it.item.mode }
            _modeSettings.value =
                modeSettings.filter { it.item.mode in modes }.toPersistentList() + modes.filter { (it) ->
                    it !in modesInSettings
                }.map { (it) ->
                    StableHolder(ReaderModePreference(scope, it, readerPreferences.getMode(it)))
                }
        }.launchIn(scope)
    }

    fun getDirectionChoices() =
        Direction.entries.associateWith { it.res.toPlatformString() }
            .toImmutableMap()

    fun getPaddingChoices() =
        mapOf(
            0 to MR.strings.page_padding_none.toPlatformString(),
            8 to "8 Dp",
            16 to "16 Dp",
            32 to "32 Dp",
        ).toImmutableMap()

    fun getMaxSizeChoices(direction: Direction) =
        if (direction == Direction.Right || direction == Direction.Left) {
            mapOf(
                0 to MR.strings.max_size_unrestricted.toPlatformString(),
                700 to "700 Dp",
                900 to "900 Dp",
                1100 to "1100 Dp",
            )
        } else {
            mapOf(
                0 to MR.strings.max_size_unrestricted.toPlatformString(),
                500 to "500 Dp",
                700 to "700 Dp",
                900 to "900 Dp",
            )
        }.toImmutableMap()

    fun getImageScaleChoices() =
        ImageScale.entries.associateWith { it.res.toPlatformString() }
            .toImmutableMap()

    fun getNavigationModeChoices() =
        NavigationMode.entries.associateWith { it.res.toPlatformString() }
            .toImmutableMap()
}

data class ReaderModePreference(
    val scope: CoroutineScope,
    val mode: String,
    val defaultMode: Boolean,
    val continuous: PreferenceMutableStateFlow<Boolean>,
    val direction: PreferenceMutableStateFlow<Direction>,
    val padding: PreferenceMutableStateFlow<Int>,
    val imageScale: PreferenceMutableStateFlow<ImageScale>,
    val fitSize: PreferenceMutableStateFlow<Boolean>,
    val maxSize: PreferenceMutableStateFlow<Int>,
    val navigationMode: PreferenceMutableStateFlow<NavigationMode>,
) {
    constructor(scope: CoroutineScope, mode: String, readerPreferences: ReaderModePreferences) :
        this(
            scope,
            mode,
            readerPreferences.default().get(),
            readerPreferences.continuous().asStateIn(scope),
            readerPreferences.direction().asStateIn(scope),
            readerPreferences.padding().asStateIn(scope),
            readerPreferences.imageScale().asStateIn(scope),
            readerPreferences.fitSize().asStateIn(scope),
            readerPreferences.maxSize().asStateIn(scope),
            readerPreferences.navigationMode().asStateIn(scope),
        )
}

@Composable
fun SettingsReaderScreenContent(
    modes: ImmutableMap<String, String>,
    selectedMode: PreferenceMutableStateFlow<String>,
    modeSettings: ImmutableList<StableHolder<ReaderModePreference>>,
    directionChoices: ImmutableMap<Direction, String>,
    paddingChoices: ImmutableMap<Int, String>,
    getMaxSizeChoices: (Direction) -> ImmutableMap<Int, String>,
    imageScaleChoices: ImmutableMap<ImageScale, String>,
    navigationModeChoices: ImmutableMap<NavigationMode, String>,
    general: ReaderGeneralPreferences,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.settings_reader_screen))
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
                    ChoicePreference(general.background, general.backgroundChoices, stringResource(MR.strings.reader_background))
                }
                item {
                    SwitchPreference(
                        general.keepScreenOn,
                        stringResource(MR.strings.reader_keep_screen_on),
                    )
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_navigation))
                }
                item {
                    SwitchPreference(general.volumeKeys, stringResource(MR.strings.reader_volume_keys))
                }
                item {
                    val volumeKeys by general.volumeKeys.collectAsState()
                    SwitchPreference(
                        general.volumeKeysInverted,
                        stringResource(MR.strings.reader_volume_keys_inverted),
                        enabled = volumeKeys,
                    )
                }
                item {
                    ChoicePreference(
                        general.tappingInvertMode,
                        general.tappingInvertChoices,
                        stringResource(MR.strings.reader_tap_invert),
                    )
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_saved_images))
                }
                item {
                    ChoicePreference(general.imageSaveTarget, general.imageSaveTargetChoices, stringResource(MR.strings.image_save_target))
                }
                item {
                    ImageSaveFolderPreference(general.imageSaveFolder)
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_default_mode))
                }
                item {
                    ChoicePreference(
                        selectedMode,
                        modes,
                        stringResource(MR.strings.reader_mode),
                    )
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_mode_options))
                }
                modeSettings.fastForEach { (it) ->
                    item {
                        ModeOptionsCard(readerModeLabel(it.mode)) {
                            ChoicePreference(
                                it.direction,
                                directionChoices,
                                stringResource(MR.strings.direction),
                                enabled = !it.defaultMode,
                            )
                            SwitchPreference(
                                it.continuous,
                                stringResource(MR.strings.continuous),
                                stringResource(MR.strings.continuous_sub),
                                enabled = !it.defaultMode,
                            )
                            val continuous by it.continuous.collectAsState()
                            if (continuous) {
                                ChoicePreference(
                                    it.padding,
                                    paddingChoices,
                                    stringResource(MR.strings.page_padding),
                                )
                                val direction by it.direction.collectAsState()
                                val (title, subtitle) = if (direction.isVertical) {
                                    stringResource(MR.strings.force_fit_width) to stringResource(MR.strings.force_fit_width_sub)
                                } else {
                                    stringResource(MR.strings.force_fit_height) to stringResource(MR.strings.force_fit_height_sub)
                                }
                                SwitchPreference(
                                    it.fitSize,
                                    title,
                                    subtitle,
                                )
                                val maxSize by it.maxSize.collectAsState()
                                val (maxSizeTitle, maxSizeSubtitle) = if (direction.isVertical) {
                                    stringResource(MR.strings.max_width) to stringResource(
                                        MR.strings.max_width_sub,
                                        maxSize,
                                    )
                                } else {
                                    stringResource(MR.strings.max_height) to stringResource(
                                        MR.strings.max_height_sub,
                                        maxSize,
                                    )
                                }
                                ChoicePreference(
                                    it.maxSize,
                                    getMaxSizeChoices(direction),
                                    maxSizeTitle,
                                    maxSizeSubtitle,
                                )
                            } else {
                                ChoicePreference(
                                    it.imageScale,
                                    imageScaleChoices,
                                    stringResource(MR.strings.image_scale),
                                )
                            }
                            ChoicePreference(
                                it.navigationMode,
                                navigationModeChoices,
                                stringResource(MR.strings.navigation_mode),
                            )
                        }
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

@Composable
private fun ModeOptionsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(bottom = 8.dp), content = content)
            }
        }
    }
}
