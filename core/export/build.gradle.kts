plugins {
    id("petru.android.library")
    id("petru.android.hilt")
}

android {
    namespace = "com.petruvideo.core.export"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("androidTest").assets.directories.add("$rootDir/core/media/src/androidTest/assets")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.exoplayer)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotest.assertions)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
