plugins {
    id("nltimer.android.library")
    id("nltimer.android.hilt")
}

android {
    namespace = "com.nltimer.core.tools"

    testOptions {
        unitTests {
            // 让单元测试中调用的 android.util.Log 等框架方法返回默认值而非抛 RuntimeException
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // 数据层 Repository 接口（业务工具调用入口）
    implementation(projects.core.data)

    // 仅为满足 nltimer.android.library 约定插件强制启用的 Compose Compiler
    // 对 Compose Runtime 在 classpath 上的存在性校验；本模块自身不使用 Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
