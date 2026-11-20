/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import dev.bunko.core.prefs.getAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import dev.bunko.ui.base.theme.AppTheme
import dev.bunko.ui.reader.LocalOrientationController
import dev.bunko.ui.reader.OrientationController
import dev.bunko.ui.reader.OrientationLock
import dev.bunko.ui.reader.ReaderMenu

class ReaderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
        )
        super.onCreate(savedInstanceState)
        val appComponent = AppComponent.getInstance(applicationContext)
        val hooks = appComponent.hooks

        val mangaId = intent.extras!!.getLong("manga", -1)
        val chapterId = intent.extras!!.getLong("chapter", -1)
        if (mangaId == -1L || chapterId == -1L) {
            finish()
            return
        }

        appComponent.readerPreferences.keepScreenOn().getAsFlow()
            .onEach {
                if (it) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            .launchIn(lifecycleScope)

        setContent {
            var lock by rememberSaveable { mutableStateOf(OrientationLock.Unlocked) }
            val controller = remember(lock) {
                OrientationController(lock) {
                    lock = it
                    requestedOrientation = it.toRequestedOrientation()
                }
            }
            CompositionLocalProvider(*hooks, LocalOrientationController provides controller) {
                AppTheme {
                    ThemedSystemBars()
                    ReaderMenu(
                        chapterId = chapterId,
                        mangaId = mangaId,
                        onCloseRequest = onBackPressedDispatcher::onBackPressed,
                    )
                }
            }
        }
    }

    private fun OrientationLock.toRequestedOrientation() = when (this) {
        OrientationLock.Unlocked -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        OrientationLock.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        OrientationLock.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}
