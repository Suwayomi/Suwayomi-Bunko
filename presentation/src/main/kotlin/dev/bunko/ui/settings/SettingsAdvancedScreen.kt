/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import dev.bunko.ui.base.userMessage
import androidx.compose.foundation.layout.Box
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogMessage
import dev.bunko.ui.base.dialog.rememberDialogState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.bunko.core.lang.bytesIntoHumanReadable
import dev.bunko.core.lang.launchIO
import dev.bunko.domain.settings.interactor.ClearServerCache
import dev.bunko.domain.updates.service.UpdatePreferences
import dev.bunko.i18n.MR
import dev.bunko.ui.base.ChapterCache
import dev.bunko.ui.base.ImageCache
import dev.bunko.ui.base.offline.OfflineChapters
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.ui.base.prefs.SwitchPreference
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
import com.diamondedge.logging.logging
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration.Companion.seconds

class SettingsAdvancedScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { settingsAdvancedViewModel() }
        SettingsAdvancedScreenContent(
            updatesEnabled = vm.updatesEnabled,
            imageCacheSize = vm.imageCacheSize.collectAsState().value,
            clearImageCache = vm::clearImageCache,
            chapterCacheSize = vm.chapterCacheSize.collectAsState().value,
            clearChapterCache = vm::clearChapterCache,
            offlineSize = vm.offlineSize.collectAsState().value,
            clearOffline = vm::clearOffline,
            clearServerPages = vm::clearServerPages,
            clearServerThumbnails = vm::clearServerThumbnails,
        )
    }
}

@Inject
class SettingsAdvancedViewModel(
    updatePreferences: UpdatePreferences,
    private val imageCache: ImageCache,
    private val chapterCache: ChapterCache,
    private val offlineChapters: OfflineChapters,
    private val clearServerCache: ClearServerCache,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val updatesEnabled = updatePreferences.enabled().asStateFlow()

    val imageCacheSize = flow {
        while (currentCoroutineContext().isActive) {
            emit(imageCache.size.bytesIntoHumanReadable())
            delay(1.seconds)
        }
    }.stateIn(scope, SharingStarted.Eagerly, "")

    val chapterCacheSize = flow {
        while (currentCoroutineContext().isActive) {
            emit(chapterCache.size.bytesIntoHumanReadable())
            delay(1.seconds)
        }
    }.stateIn(scope, SharingStarted.Eagerly, "")

    val offlineSize = flow {
        while (currentCoroutineContext().isActive) {
            emit(withContext(Dispatchers.IO) { offlineChapters.store.sizeBytes() }.bytesIntoHumanReadable())
            delay(2.seconds)
        }
    }.stateIn(scope, SharingStarted.Eagerly, "")

    fun clearOffline() = offlineChapters.deleteAll()

    fun clearServerPages() = clearServer(pages = true, thumbnails = false)

    fun clearServerThumbnails() = clearServer(pages = false, thumbnails = true)

    private fun clearServer(
        pages: Boolean,
        thumbnails: Boolean,
    ) {
        scope.launchIO {
            val ok = clearServerCache.await(pages, thumbnails, onError = { toast(it.userMessage.toPlatformString()) })
            if (ok != null) toast(MR.strings.server_cache_cleared.toPlatformString())
        }
    }

    fun clearImageCache() {
        scope.launchIO {
            imageCache.clear()
        }
    }

    fun clearChapterCache() {
        scope.launchIO {
            chapterCache.clear()
        }
    }

    companion object {
        private val log = logging()
    }
}

@Composable
fun SettingsAdvancedScreenContent(
    updatesEnabled: PreferenceMutableStateFlow<Boolean>,
    imageCacheSize: String,
    clearImageCache: () -> Unit,
    chapterCacheSize: String,
    clearChapterCache: () -> Unit,
    offlineSize: String,
    clearOffline: () -> Unit,
    clearServerPages: () -> Unit,
    clearServerThumbnails: () -> Unit,
) {
    val confirmOffline = rememberDialogState()
    AppDialog(
        state = confirmOffline,
        title = stringResource(MR.strings.offline_clear_all),
        confirm = DialogButton(stringResource(MR.strings.action_delete), onClick = clearOffline),
        dismissText = stringResource(MR.strings.action_cancel),
    ) {
        DialogMessage(stringResource(MR.strings.offline_clear_all_confirm))
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.settings_advanced_screen))
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
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_updates))
                }
                item {
                    SwitchPreference(preference = updatesEnabled, title = stringResource(MR.strings.update_checker))
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_storage))
                }
                item {
                    PreferenceRow(
                        title = stringResource(MR.strings.clear_image_cache),
                        subtitle = stringResource(MR.strings.clear_cache_sub, imageCacheSize),
                        onClick = clearImageCache,
                    )
                }
                item {
                    PreferenceRow(
                        title = stringResource(MR.strings.clear_chapter_cache),
                        subtitle = stringResource(MR.strings.clear_cache_sub, chapterCacheSize),
                        onClick = clearChapterCache,
                    )
                }
                item {
                    PreferenceRow(
                        title = stringResource(MR.strings.offline_clear_all),
                        subtitle = stringResource(MR.strings.clear_cache_sub, offlineSize),
                        onClick = confirmOffline::show,
                    )
                }
                item {
                    PreferenceSectionHeader(stringResource(MR.strings.settings_section_server_storage))
                }
                item {
                    PreferenceRow(
                        title = stringResource(MR.strings.server_clear_pages),
                        subtitle = stringResource(MR.strings.server_clear_pages_sub),
                        onClick = clearServerPages,
                    )
                }
                item {
                    PreferenceRow(
                        title = stringResource(MR.strings.server_clear_thumbnails),
                        subtitle = stringResource(MR.strings.server_clear_thumbnails_sub),
                        onClick = clearServerThumbnails,
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
