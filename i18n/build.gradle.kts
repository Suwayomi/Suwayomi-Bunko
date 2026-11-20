plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.moko.gradle.get().pluginId)
}

kotlin {
    androidTarget()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.moko.core)
            }
        }
    }
}

tasks {
    registerLocalizationTask(project)
}

multiplatformResources {
    resourcesPackage = "dev.bunko.i18n"
}

android {
    namespace = "dev.bunko.i18n"
    lint {
        disable += "MissingTranslation"
    }

    sourceSets.getByName("main") {
        assets.srcDir(File(layout.buildDirectory.asFile.get(), "generated/moko/androidMain/assets"))
        res.srcDir(File(layout.buildDirectory.asFile.get(), "generated/moko/androidMain/res"))
    }
}
