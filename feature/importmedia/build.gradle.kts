plugins {
    id("petru.android.library")
    id("petru.android.compose")
    id("petru.android.hilt")
}

android {
    namespace = "com.petruvideo.feature.importmedia"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
}
