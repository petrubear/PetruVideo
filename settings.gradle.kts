pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PetruVideo"

include(":app")
include(":core:model")
include(":core:data")
include(":core:media")
include(":core:designsystem")
include(":feature:projects")
include(":feature:importmedia")
include(":feature:editor")
include(":core:export")
include(":feature:export")

// Macrobenchmarks need extra artifacts and a physical device; enable with -Pbenchmark.
if (providers.gradleProperty("benchmark").isPresent) include(":benchmark")
