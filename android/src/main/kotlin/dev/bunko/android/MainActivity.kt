package dev.bunko.android

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.bunko.ui.base.theme.AppTheme
import dev.bunko.ui.main.MainMenu

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
        )
        super.onCreate(savedInstanceState)

        val appComponent = AppComponent.getInstance(applicationContext)
        if (savedInstanceState == null) {
            appComponent.migrations.runMigrations()
            appComponent.appMigrations.runMigrations()
        }

        if (!isTaskRoot) {
            finish()
            return
        }

        val uiHooks = appComponent.hooks
        setContent {
            CompositionLocalProvider(*uiHooks) {
                AppTheme {
                    ThemedSystemBars()
                    MainMenu()
                }
            }
        }
    }
}
