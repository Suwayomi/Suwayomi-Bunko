pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
rootProject.name = "Bunko"

include("core")
include("i18n")
include("data")
include("domain")
include("ui-core")
include("presentation")
include("android")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
