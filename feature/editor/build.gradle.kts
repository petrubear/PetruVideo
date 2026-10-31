plugins {
    id("petru.android.library")
    id("petru.android.compose")
    id("petru.android.hilt")
    id("petru.android.screenshots")
}

android {
    namespace = "com.petruvideo.feature.editor"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
}
