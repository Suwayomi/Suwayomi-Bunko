plugins {
    groovy
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.android)
    implementation(libs.gradle.baselineprofile)
}
