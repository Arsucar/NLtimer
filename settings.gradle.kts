pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NLtimer"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "app",
    "core:ai",
    "core:designsystem",
    "core:data",
    "core:behaviorui",
    "core:debugui",
    "core:tools",
    "feature:ai",
    "feature:home",
    "feature:sub",
    "feature:stats",
    "feature:settings",
    "feature:categories",
    "feature:management_activities",
    "feature:tag_management",
    "feature:behavior_management",
    "feature:debug",
)
