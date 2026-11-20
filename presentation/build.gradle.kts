import org.jetbrains.compose.compose

plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.compose.get().pluginId)
    id(libs.plugins.compose.get().pluginId)
    id(libs.plugins.ksp.get().pluginId)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "kotlin.time.ExperimentalTime",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
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
    api(libs.voyager.navigation)
    api(libs.voyager.transitions)
    api(libs.voyager.screenmodel)
    api(libs.dateTime)
    api(libs.immutableCollections)
    api(libs.aboutLibraries.core)
    api(libs.aboutLibraries.ui)

    api(projects.core)
    api(projects.i18n)
    api(projects.domain)
    api(projects.uiCore)
    api(compose.runtime)
    api(compose.foundation)
    api(compose.ui)
    api(compose.material)
    api(compose.material3)
    api(compose("org.jetbrains.compose.ui:ui-util"))
    api(compose.materialIconsExtended)

    api(libs.androidx.core)
    api(libs.androidx.appCompat)
    api(libs.androidx.activity.compose)

    ksp(libs.kotlinInject.compiler)
}

android {
    namespace = "dev.bunko.presentation"
    appBuildConfig(project)
}
