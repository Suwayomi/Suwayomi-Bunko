import org.jetbrains.compose.compose

plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.ksp.get().pluginId)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "kotlin.time.ExperimentalTime",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "com.russhwolf.settings.ExperimentalSettingsApi",
            "com.russhwolf.settings.ExperimentalSettingsImplementation",
        )
    }
}

dependencies {
    api(libs.coroutines.core)
    api(libs.serialization.json.core)
    api(libs.kotlinInject.runtime)
    api(libs.ktor.core)
    api(libs.ktor.contentNegotiation)
    api(libs.ktor.serialization.json)
    api(libs.okio)
    api(libs.logging.kmlogging)
    api(libs.multiplatformSettings.core)
    api(libs.multiplatformSettings.serialization)
    api(libs.dateTime)
    api(compose("org.jetbrains.compose.ui:ui-text"))
    ksp(libs.kotlinInject.compiler)
}

android {
    namespace = "dev.bunko.core"
}
