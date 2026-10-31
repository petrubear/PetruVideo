plugins {
    `kotlin-dsl`
}

group = "com.petruvideo.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.paparazzi.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "petru.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "petru.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "petru.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "petru.android.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("screenshots") {
            id = "petru.android.screenshots"
            implementationClass = "ScreenshotTestConventionPlugin"
        }
        register("jvmLibrary") {
            id = "petru.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
