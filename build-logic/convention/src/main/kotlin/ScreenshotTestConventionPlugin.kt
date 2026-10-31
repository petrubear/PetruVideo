import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Golden-image screenshot tests (Paparazzi, rendered on the JVM). Goldens are generated from the Compose UI and
 * approved by the designer; they are never pixel-compared with the designer's mock images (research 9).
 *
 * - `./gradlew recordScreenshots` writes or updates the goldens under `src/test/snapshots`.
 * - `./gradlew verifyScreenshots` fails when a screen no longer matches its golden.
 */
class ScreenshotTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(target.plugin("paparazzi"))
        target.tasks.register("recordScreenshots") {
            group = "verification"
            description = "Records golden screenshots for this module"
            dependsOn("recordPaparazziDebug")
        }
        target.tasks.register("verifyScreenshots") {
            group = "verification"
            description = "Verifies this module's screens against the approved golden screenshots"
            dependsOn("verifyPaparazziDebug")
        }
    }
}
