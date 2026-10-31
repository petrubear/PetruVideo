plugins {
    id("petru.android.library")
    id("petru.android.compose")
}

android {
    namespace = "com.petruvideo.core.designsystem"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.window)
    api(libs.compose.material3.window.size)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.kotlinx.coroutines.android)
}
