import Config.migrationCode
import java.util.Properties

class ReleaseSigning(val storeFile: File, val storePassword: String, val keyAlias: String, val keyPassword: String)

fun readReleaseSigning(): ReleaseSigning? {
    val props = Properties()
    rootProject.file("keystore.properties").takeIf { it.isFile }?.reader()?.use(props::load)
    fun value(prop: String, env: String): String? =
        (props.getProperty(prop) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

    val values = listOf(
        value("storeFile", "BUNKO_KEYSTORE_FILE"),
        value("storePassword", "BUNKO_KEYSTORE_PASSWORD"),
        value("keyAlias", "BUNKO_KEY_ALIAS"),
        value("keyPassword", "BUNKO_KEY_PASSWORD"),
    )
    if (values.all { it == null }) return null
    val (file, storePassword, alias, keyPassword) = values.map {
        it ?: throw GradleException("release signing is partially configured: set storeFile, storePassword, keyAlias and keyPassword")
    }
    val store = rootProject.file(file)
    if (!store.isFile) throw GradleException("release keystore not found: $store")
    return ReleaseSigning(store, storePassword, alias, keyPassword)
}

val releaseSigning = readReleaseSigning()

plugins {
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.compose.get().pluginId)
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.ksp.get().pluginId)
    id(libs.plugins.compose.get().pluginId)
    id(libs.plugins.aboutLibraries.get().pluginId)
}

dependencies {
    implementation(projects.core)
    implementation(projects.i18n)
    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.uiCore)
    implementation(projects.presentation)

    implementation(libs.androidx.work)
    implementation(libs.lifecycle.process)
    implementation(libs.lifecycle.runtime)
    implementation(libs.apollo.runtime)

    ksp(libs.kotlinInject.compiler)
}

android {
    namespace = "dev.bunko.android"
    defaultConfig {
        applicationId = "dev.bunko.android"
        versionCode = migrationCode
        versionName = version.toString()
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            signingConfig = releaseSigning?.let { key ->
                signingConfigs.create("release") {
                    storeFile = key.storeFile
                    storePassword = key.storePassword
                    keyAlias = key.keyAlias
                    keyPassword = key.keyPassword
                }
            } ?: run {
                logger.warn("bunko: no release signing key configured, release build uses the debug key")
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes.addAll(listOf(
            "META-INF/DEPENDENCIES",
            "LICENSE.txt",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
            "META-INF/README.md",
            "META-INF/NOTICE",
            "META-INF/*.kotlin_module",
        ))
    }
}


aboutLibraries {
    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
}
