plugins {
    id("nltimer.android.library")
    id("nltimer.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nltimer.feature.ai"
}

dependencies {
    implementation(projects.core.ai)
    implementation(projects.core.tools)
    implementation(projects.core.designsystem)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.compose.markdown)
    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
    implementation(libs.intellij.markdown)
    implementation(libs.jsoup)
    implementation(libs.quickjs.android)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.collections.immutable)
}
