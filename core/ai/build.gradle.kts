plugins {
    id("nltimer.android.library")
    id("nltimer.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nltimer.core.ai"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.datastore.preferences)

    implementation(projects.core.tools)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
