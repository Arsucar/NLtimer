plugins {
    id("nltimer.android.application")
    id("nltimer.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val APP_VERSION_NAME: String by project
val APP_VERSION_CODE: String by project
val APP_ID: String by project

android {
    namespace = APP_ID

    defaultConfig {
        applicationId = APP_ID
        versionCode = APP_VERSION_CODE.toInt()
        versionName = APP_VERSION_NAME
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        val localProperties = com.android.build.gradle.internal.cxx.configure.gradleLocalProperties(rootDir, providers)
        val releaseKeystorePath = localProperties.getProperty("keystore.path", System.getenv("KEYSTORE_PATH") ?: "")
        val releaseKeystorePassword = localProperties.getProperty("keystore.password", System.getenv("KEYSTORE_PASSWORD") ?: "")
        val releaseKeyAlias = localProperties.getProperty("key.alias", System.getenv("KEY_ALIAS") ?: "")
        val releaseKeyPassword = localProperties.getProperty("key.password", System.getenv("KEY_PASSWORD") ?: "")

        getByName("debug") {
            isDebuggable = true
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (releaseKeystorePath.isNotBlank()) {
                signingConfigs.create("release") {
                    storeFile = file(releaseKeystorePath)
                    storePassword = releaseKeystorePassword
                    keyAlias = releaseKeyAlias
                    keyPassword = releaseKeyPassword
                }
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.feature.home)
    implementation(projects.feature.stats)
    implementation(projects.feature.settings)
    implementation(projects.feature.categories)
    implementation(projects.feature.managementActivities)
    implementation(projects.feature.tagManagement)
    implementation(projects.feature.behaviorManagement)
    debugImplementation(projects.feature.debug)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.okio)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.profileinstaller)
    implementation(libs.splashscreen)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    testImplementation(libs.junit)

    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
}
