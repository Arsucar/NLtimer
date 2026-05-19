plugins {
    id("nltimer.android.library")
}

android {
    namespace = "com.nltimer.core.designsystem"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3.expressive)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.materialkolor)
    implementation(libs.colorpicker.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.hugeicons.compose)
}
