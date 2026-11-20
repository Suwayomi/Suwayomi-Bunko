/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.bunko.i18n.MR
import dev.bunko.ui.downloads.DownloadsScreen
import dev.bunko.ui.extensions.ExtensionsScreen
import dev.bunko.ui.library.LibraryScreen
import dev.bunko.ui.main.about.AboutScreen
import dev.bunko.ui.main.components.DownloadsExtraInfo
import dev.bunko.ui.main.components.LibraryUpdatesExtraInfo
import dev.bunko.ui.main.more.MoreScreen
import dev.bunko.ui.offline.OfflineScreen
import dev.bunko.ui.sources.SourcesScreen
import dev.bunko.ui.history.HistoryScreen
import dev.bunko.ui.schedule.ScheduleScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import dev.icerock.moko.resources.StringResource
import kotlin.reflect.KClass

interface Menu {
    val textKey: StringResource
    val unselectedIcon: ImageVector
    val selectedIcon: ImageVector
    val screen: KClass<*>
    val createScreen: () -> Screen
    val extraInfo: (@Composable () -> Unit)?

    fun isSelected(navigator: Navigator) = navigator.items.first()::class == screen
}

enum class TopLevelMenus(
    override val textKey: StringResource,
    override val unselectedIcon: ImageVector,
    override val selectedIcon: ImageVector,
    override val screen: KClass<*>,
    override val createScreen: () -> Screen,
    override val extraInfo: (@Composable () -> Unit)? = null,
    private val alsoSelectedOn: List<KClass<*>> = emptyList(),
) : Menu {
    Library(
        MR.strings.location_library,
        Icons.Outlined.CollectionsBookmark,
        Icons.Filled.CollectionsBookmark,
        LibraryScreen::class,
        { LibraryScreen() },
        extraInfo = { LibraryUpdatesExtraInfo() },
    ),
    History(
        MR.strings.location_history,
        Icons.Outlined.History,
        Icons.Filled.History,
        HistoryScreen::class,
        { HistoryScreen() },
    ),
    Schedule(
        MR.strings.location_schedule,
        Icons.Outlined.CalendarMonth,
        Icons.Filled.CalendarMonth,
        ScheduleScreen::class,
        { ScheduleScreen() },
    ),
    Browse(
        MR.strings.location_browse,
        Icons.Outlined.Explore,
        Icons.Filled.Explore,
        SourcesScreen::class,
        { SourcesScreen() },
        alsoSelectedOn = listOf(ExtensionsScreen::class),
    ),
    More(
        MR.strings.location_more,
        Icons.Outlined.MoreHoriz,
        Icons.Filled.MoreHoriz,
        MoreScreen::class,
        { MoreScreen() },
    ),
    ;

    override fun isSelected(navigator: Navigator): Boolean {
        val current = navigator.items.first()::class
        return current == screen || current in alsoSelectedOn
    }
}

enum class MoreMenus(
    override val textKey: StringResource,
    override val unselectedIcon: ImageVector,
    override val selectedIcon: ImageVector,
    override val screen: KClass<*>,
    override val createScreen: () -> Screen,
    override val extraInfo: (@Composable () -> Unit)? = null,
) : Menu {
    Downloads(MR.strings.settings_downloads_storage, Icons.Outlined.Download, Icons.Rounded.Download, DownloadsScreen::class, {
        DownloadsScreen()
    }, extraInfo = { DownloadsExtraInfo() }),
    Offline(MR.strings.more_offline_chapters, Icons.Outlined.PhoneAndroid, Icons.Rounded.PhoneAndroid, OfflineScreen::class, { OfflineScreen() }),
    About(MR.strings.location_about, Icons.Outlined.Info, Icons.Rounded.Info, AboutScreen::class, { AboutScreen() }),
}
