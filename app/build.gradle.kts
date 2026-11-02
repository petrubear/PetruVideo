plugins {
    id("petru.android.application")
    id("petru.android.compose")
    id("petru.android.hilt")
}

// The one place the version is set (see CHANGELOG.md). The version code follows from it: 0.2.1 is 201, 1.3.4 is 10304.
val appVersion = "0.2.1"
val appVersionCode = appVersion.split(".").map { it.toInt() }.let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }

android {
    namespace = "com.petruvideo.app"

    defaultConfig {
        applicationId = "com.petruvideo"
        versionCode = appVersionCode
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Release-like and signed with the debug key, for the macrobenchmark module.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:projects"))
    implementation(project(":feature:importmedia"))
    implementation(project(":feature:editor"))
    implementation(project(":core:export"))
    implementation(project(":feature:export"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
}

dependencies {
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotest.assertions)
}

// The manifest test reads the merged manifest, so make sure it is built first.
tasks.matching { it.name == "testDebugUnitTest" }.configureEach { dependsOn("processDebugMainManifest") }
