import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(plugin("kotlin-compose"))
        extensions.getByType<CommonExtension>().buildFeatures.compose = true
        dependencies {
            val bom = platform(lib("compose-bom"))
            add("implementation", bom)
            add("androidTestImplementation", bom)
            add("implementation", lib("compose-ui"))
            add("implementation", lib("compose-foundation"))
            add("implementation", lib("compose-material3"))
            add("implementation", lib("compose-ui-tooling-preview"))
            add("debugImplementation", lib("compose-ui-tooling"))
            add("androidTestImplementation", lib("compose-ui-test-junit4"))
            add("debugImplementation", lib("compose-ui-test-manifest"))
        }
    }
}
