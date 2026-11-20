/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main

import dev.bunko.domain.ui.model.StartScreen
import dev.bunko.ui.extensions.ExtensionsScreen
import dev.bunko.ui.library.LibraryScreen
import dev.bunko.ui.sources.SourcesScreen
import dev.bunko.ui.history.HistoryScreen
import dev.bunko.ui.schedule.ScheduleScreen

fun StartScreen.toScreen() =
    when (this) {
        StartScreen.Library -> LibraryScreen()
        StartScreen.History -> HistoryScreen()
        StartScreen.Schedule -> ScheduleScreen()
        StartScreen.Sources -> SourcesScreen()
        StartScreen.Extensions -> ExtensionsScreen()
    }

fun StartScreen.toScreenClazz() =
    when (this) {
        StartScreen.Library -> LibraryScreen::class
        StartScreen.History -> HistoryScreen::class
        StartScreen.Schedule -> ScheduleScreen::class
        StartScreen.Sources -> SourcesScreen::class
        StartScreen.Extensions -> ExtensionsScreen::class
    }
