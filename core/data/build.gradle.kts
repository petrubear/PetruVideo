plugins {
    id("petru.android.library")
    id("petru.android.hilt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.petruvideo.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.room.testing)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
