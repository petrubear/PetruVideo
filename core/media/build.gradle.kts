plugins {
    id("petru.android.library")
    id("petru.android.hilt")
}

android {
    namespace = "com.petruvideo.core.media"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.transformer)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
}
