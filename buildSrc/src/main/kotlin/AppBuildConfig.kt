import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project

fun LibraryExtension.appBuildConfig(project: Project) {
    buildFeatures.buildConfig = true
    defaultConfig {
        buildConfigField("String", "NAME", "\"${project.rootProject.name}\"")
        buildConfigField("String", "VERSION", "\"${project.version}\"")
        buildConfigField("int", "MIGRATION_CODE", Config.migrationCode.toString())
    }
}
