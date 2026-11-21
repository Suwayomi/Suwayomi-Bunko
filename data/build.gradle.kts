import com.google.devtools.ksp.gradle.KspAATask

plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
    id(libs.plugins.ksp.get().pluginId)
    id(libs.plugins.apollo.get().pluginId)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

dependencies {
    api(libs.coroutines.core)
    api(libs.serialization.json.core)
    api(libs.kotlinInject.runtime)
    api(libs.ktor.core)
    api(libs.ktor.websockets)
    api(libs.okio)
    api(libs.dateTime)
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.cache)
    implementation(libs.apollo.cache.sqlite)
    implementation(libs.apollo.engine.ktor)
    api(projects.core)
    api(projects.i18n)
    api(projects.domain)
    ksp(libs.kotlinInject.compiler)
}

android {
    namespace = "dev.bunko.data"
}

apollo {
    service("service") {
        packageName.set("dev.bunko.data.graphql")
        generateMethods.set(listOf("equalsHashCode"))
        mapScalar("LongString","kotlin.Long", "dev.bunko.data.scalars.LongStringScalar")
        mapScalarToUpload("Upload")
        introspection {
            endpointUrl.set("http://localhost:4567/api/graphql")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}

tasks.withType<KspAATask> {
    mustRunAfter("generateServiceApolloSources")
}
