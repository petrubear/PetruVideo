import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            compileSdk = 37
            defaultConfig.minSdk = 26
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            testOptions.unitTests.isReturnDefaultValues = true
        }
        configureDetekt()
        dependencies {
            add("testImplementation", lib("junit"))
            add("testImplementation", lib("kotest-assertions"))
            add("testImplementation", lib("turbine"))
            add("testImplementation", lib("kotlinx-coroutines-test"))
        }
    }
}
