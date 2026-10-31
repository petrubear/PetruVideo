plugins {
    id("petru.android.library")
    id("petru.android.hilt")
}

android {
    namespace = "com.petruvideo.core.media"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.transformer)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotest.assertions)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
