plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:gradle:9.2.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    implementation("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.3.21")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.59.2")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.8")
}
