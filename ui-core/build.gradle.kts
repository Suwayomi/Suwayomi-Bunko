import org.jetbrains.compose.compose

plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.compose.get().pluginId)
    id(libs.plugins.compose.get().pluginId)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "kotlin.time.ExperimentalTime",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.material.ExperimentalMaterialApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }
}

dependencies {
    api(libs.coroutines.core)
    api(libs.imageloader.core)
    api(libs.imageloader.moko)
    api(libs.voyager.core)
    api(libs.voyager.screenmodel)
    api(libs.dateTime)
    api(libs.immutableCollections)
    api(projects.core)
    api(projects.i18n)
    api(compose.runtime)
    api(compose.foundation)
    api(compose.ui)
    api(compose.material)
    api(compose.material3)
    api(compose.materialIconsExtended)
    api(compose("org.jetbrains.compose.ui:ui-util"))
    api(libs.androidx.core)
    api(libs.androidx.appCompat)
}

android {
    namespace = "dev.bunko.uicore"
}
