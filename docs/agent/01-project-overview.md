# 项目概览

| 项 | 值 |
|---|---|
| 名称 | NLtimer |
| 定位 | Android 行为记录与时间管理应用 |
| 版本 | 0.3.12 (build 312) |
| 包名 | `com.nltimer.app` |
| 语言 | Kotlin 2.3.21 |
| UI | Jetpack Compose + Material 3 (Expressive) |
| 构建 | AGP 9.2.0, Gradle Kotlin DSL, Version Catalog |
| minSdk / targetSdk | 31 / 36 |
| DI | Hilt 2.59.2 (KSP) |
| DB | Room 2.7.1, 版本 17, 13 表 |
| 导航 | Navigation Compose 2.9.0 |
| 序列化 | Kotlinx Serialization 1.8.1 |
| 存储 | DataStore Preferences 1.2.1 |
| 主题 | MaterialKolor 4.1.1 (动态调色板) |
| 网络 | OkHttp 4.12.0 + SSE |
| 架构 | MVVM, 单 Activity, 多模块 |
| 静态分析 | Detekt 1.23.8 |
| CI | GitHub Actions (tag-triggered release) |

## Agent 构建注意事项

> **Gradle Daemon 会阻塞 Agent / CI 进程退出**

Gradle 默认启动后台守护进程（Daemon），构建完成后进程仍在运行。Agent 或 CI 中的 CLI 会等待所有子进程结束才返回，导致看似 "BUILD SUCCESSFUL" 后卡住不退出。

**规则：Agent 执行 Gradle 构建命令时，必须加 `--no-daemon` 参数。**

```bash
# 正确
./gradlew :app:compileDebugKotlin --no-daemon

# 错误（会卡住）
./gradlew :app:compileDebugKotlin
```

备选：构建完成后执行 `./gradlew --stop` 强制关闭残留 Daemon。
