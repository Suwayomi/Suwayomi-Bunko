import org.jetbrains.compose.compose

plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
    id(libs.plugins.ksp.get().pluginId)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "kotlin.time.ExperimentalTime",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

dependencies {
    api(libs.coroutines.core)
    api(libs.serialization.json.core)
    api(libs.kotlinInject.runtime)
    api(libs.ktor.core)
    api(libs.ktor.okHttp)
    api(libs.ktor.contentNegotiation)
    api(libs.ktor.serialization.json)
    api(libs.ktor.logging)
    api(libs.ktor.websockets)
    api(libs.okio)
    api(libs.dateTime)
    api(compose("org.jetbrains.compose.runtime:runtime"))
    api(projects.core)
    api(projects.i18n)
    ksp(libs.kotlinInject.compiler)
}

android {
    namespace = "dev.bunko.domain"
    appBuildConfig(project)
}
