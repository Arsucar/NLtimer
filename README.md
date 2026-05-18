# NLtimer

![Build](https://github.com/Free-sss/NLtimer/actions/workflows/publish-release.yaml/badge.svg)

NLtimer 是一款 Android 行为记录与时间管理应用，用于快速记录当前行为、补记已完成行为、管理活动与标签，并通过多种主页视图复盘时间使用情况。

## 特性

- **行为计时：** 支持开始当前行为、完成行为、补记已完成行为和记录目标行为。
- **时间视图：** 首页支持网格、时间轴、反向时间线和 Moment 等展示方式。
- **活动管理：** 支持活动分组、活动归档、颜色、图标、关键词和使用次数等信息。
- **标签管理：** 支持标签创建、筛选、移动与管理，用于细化行为记录。
- **备注匹配：** 可从备注中匹配活动与标签，降低手动选择成本。
- **统计与复盘：** 提供统计页面，用于查看行为记录与时间分布。
- **个性化配置：** 支持主题、色板、弹窗网格、首页布局和时间标签配置。
- **数据管理：** 支持应用数据导入、导出与迁移。
- **Debug 工具：** Debug 构建可注入调试页面，Release 构建不包含调试入口。

## 技术栈

- Kotlin 2.3.21
- Android Gradle Plugin 9.2.0
- Jetpack Compose + Material 3
- Navigation Compose
- Hilt + KSP
- Room
- DataStore Preferences
- Kotlinx Serialization
- JUnit、MockK、Turbine、Compose UI Test
- Detekt

## 环境要求

- Android Studio：建议使用支持 AGP 9.x 的版本。
- JDK：17。
- Android SDK：compileSdk 36，targetSdk 36，minSdk 31。
- Gradle：使用仓库内置 Gradle Wrapper。

## 快速开始

### 克隆项目

```bash
git clone https://github.com/Free-sss/NLtimer.git
cd NLtimer
```

### 构建 Debug APK

Windows：

```powershell
.\gradlew.bat assembleDebug
```

macOS / Linux：

```bash
./gradlew assembleDebug
```

构建产物位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 安装到设备

Windows：

```powershell
.\gradlew.bat installDebug
```

macOS / Linux：

```bash
./gradlew installDebug
```

## 常用命令

```bash
# 运行单元测试
./gradlew test

# 运行 app 模块 Debug 单元测试
./gradlew :app:testDebugUnitTest

# 运行连接设备上的 Android 测试
./gradlew connectedDebugAndroidTest

# 运行 Detekt 静态检查
./gradlew detekt

# 构建 Release APK
./gradlew assembleRelease
```

Windows 环境请将 `./gradlew` 替换为 `.\gradlew.bat`。

## 项目结构

```text
NLtimer/
├── app/                         # Android 应用入口、全局 Scaffold 与导航
├── core/
│   ├── data/                    # 数据库、Repository、UseCase、领域模型与数据导入导出
│   ├── designsystem/            # 主题、颜色、组件与设计系统
│   ├── behaviorui/              # 行为添加弹窗、活动/标签选择等复用 UI
│   ├── debugui/                 # Debug 字段检查与调试 UI
│   └── tools/                   # 行为计时、匹配、工具注册等能力
├── feature/
│   ├── home/                    # 首页时间记录与多布局展示
│   ├── stats/                   # 统计页面
│   ├── settings/                # 设置、主题、数据管理与布局配置
│   ├── categories/              # 分类页面
│   ├── management_activities/   # 活动管理
│   ├── tag_management/          # 标签管理
│   ├── behavior_management/     # 行为记录管理
│   └── debug/                   # Debug 专用功能模块
├── buildSrc/                    # 共享 Gradle 插件与 SDK 版本配置
├── gradle/                      # Version Catalog 与 Gradle Wrapper 配置
└── docs/
    ├── developer/               # 开发者文档（架构、模块、组件库等）
    ├── user/                    # 用户文档（快速上手、功能说明等）
    ├── agent/                   # Agent 文档（AI 上下文填充）
    ├── reference/               # 参考资料与竞品分析
    └── myNote/                  # 开发者个人笔记
```

## 主要模块说明

| 模块 | 说明 |
| --- | --- |
| `app` | 应用入口，负责 `MainActivity`、Hilt 初始化、全局导航和顶层 UI 容器。 |
| `core:data` | 提供活动、标签、分类、行为记录、设置和数据导入导出等数据能力。 |
| `core:designsystem` | 提供 NLtimer 主题、设计 token、通用组件和 Material 3 封装。 |
| `core:behaviorui` | 提供新增行为、活动选择、标签选择、时间选择等跨页面复用组件。 |
| `core:tools` | 提供行为计时工具、活动/标签匹配工具和工具注册表。 |
| `feature:home` | 首页行为记录主流程，包括当前行为、补记、目标、空闲时间和多种布局展示。 |
| `feature:settings` | 设置入口，包含主题配置、弹窗配置、首页布局配置、色板和数据管理。 |

## 配置说明

应用版本与包名配置在 `gradle.properties`：

```properties
APP_VERSION_NAME=0.1.5
APP_VERSION_CODE=105
APP_ID=com.nltimer.app
```

Release 构建会优先读取 `local.properties` 或环境变量中的签名配置：

```properties
keystore.path=your-release-key.jks
keystore.password=your-store-password
key.alias=your-key-alias
key.password=your-key-password
```

如果没有提供签名配置，Release 构建会回退到 Debug 签名，便于本地验证构建流程。

## CI 与发布

项目使用 GitHub Actions：

- `publish-release.yaml`：在 `main`、`feature/*` 和 Pull Request 上构建 Debug APK；推送 `v*.*.*` 标签时创建 Release。
- `opencode.yml`：在 Issue 或 Pull Request 评论中通过 `/oc` 或 `/opencode` 触发 OpenCode。

发布标签格式：

```bash
git tag v0.1.5
git push origin v0.1.5
```

CI 会从标签中提取版本号，并更新构建时的 `APP_VERSION_NAME` 与 `APP_VERSION_CODE`。

## 文档

| 文档类型 | 目录 | 说明 |
| --- | --- | --- |
| [开发者文档](docs/developer/00-索引.md) | `docs/developer/` | 架构、模块详解、数据模型、组件库、代码约定 |
| [用户文档](docs/user/00-索引.md) | `docs/user/` | 快速上手、活动/标签/行为管理、视图、设置 |
| [Agent 文档](docs/agent/00-index.md) | `docs/agent/` | 项目概览、架构摘要、模块映射、模式速查 |

## 开发约定

- 优先使用 Kotlin、Compose 和 Material 3 实现界面。
- 依赖版本统一维护在 `gradle/libs.versions.toml`。
- 共享 Gradle 逻辑放在 `buildSrc` 的预编译脚本插件中。
- 功能按 `feature:*` 模块拆分，通用能力放入 `core:*` 模块。
- Debug 专用能力放入 `feature:debug` 或 `core:debugui`，避免进入 Release 主流程。

## 许可证

当前仓库未声明许可证。如需开源分发，请先补充 `LICENSE` 文件。
