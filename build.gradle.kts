import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.moko.gradle) apply false
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.apollo) apply false
    alias(libs.plugins.versions)
}

allprojects {
    group = "dev.bunko"
    version = "1.1.0"

    dependencies {
        modules {
            module("androidx.lifecycle:lifecycle-viewmodel-ktx") {
                replacedBy("androidx.lifecycle:lifecycle-viewmodel")
            }
        }
    }
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

subprojects {
    afterEvaluate {
        extensions.findByType<KotlinProjectExtension>()?.jvmToolchain(Config.androidJvmTarget.target.toInt())
    }
    tasks.withType<KotlinJvmCompile> {
        compilerOptions {
            jvmTarget = Config.androidJvmTarget

            if (project.hasProperty("generateComposeCompilerMetrics")) {
                val metricsDir = project.layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath
                freeCompilerArgs.addAll(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$metricsDir",
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$metricsDir",
                )
            }
        }
    }
    plugins.withType<com.android.build.gradle.BasePlugin> {
        configure<com.android.build.gradle.BaseExtension> {
            compileSdkVersion(36)
            defaultConfig {
                minSdk = 21
                targetSdk = 36
            }
            compileOptions {
                isCoreLibraryDesugaringEnabled = true
                sourceCompatibility(Config.androidJvmTarget.target)
                targetCompatibility(Config.androidJvmTarget.target)
            }
            dependencies {
                add("coreLibraryDesugaring", libs.desugarJdkLibs)
            }
            buildFeatures.apply {
                aidl = false
                renderScript = false
                shaders = false
            }
        }
    }

    plugins.withType<com.google.devtools.ksp.gradle.KspGradleSubplugin> {
        configure<com.google.devtools.ksp.gradle.KspExtension> {
            arg("me.tatarka.inject.generateCompanionExtensions", "true")
            if (project.hasProperty("debugApp")) {
                arg("me.tatarka.inject.dumpGraph", "true")
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.contains(it, true) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
