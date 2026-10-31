plugins {
    id("petru.android.application")
    id("petru.android.compose")
    id("petru.android.hilt")
}

android {
    namespace = "com.petruvideo.app"

    defaultConfig {
        applicationId = "com.petruvideo"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
