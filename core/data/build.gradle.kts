plugins {
    id("petru.android.library")
    id("petru.android.hilt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.petruvideo.core.data"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:media"))
    api(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
