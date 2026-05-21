plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint)
}
detekt {
    // 如果你的配置文件在项目根目录，名字叫 detekt.yml
    //    config.setFrom(files("$rootDir/detekt.yml"))

    // 如果你想让它只报警告不卡编译，也可以在这里全局控制
    ignoreFailures = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // 告诉 detekt 包含所有子模块的 src 目录
    setSource(files(rootDir))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")
}

// 全局配置子模块的 ktlint
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(false)
        verbose.set(true)
        android.set(true) // 开启 Android 特定的代码规范检查（比如大括号换行规则）
        outputToConsole.set(true)
        ignoreFailures.set(true) // 建议设为 true，这样哪怕有格式问题，也不会直接卡死你的 build 编译
    }
}
