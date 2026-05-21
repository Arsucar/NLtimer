# 提取 ai_inter 为独立 feature:ai 模块

## 目标

将 `app/src/main/java/com/nltimer/app/experimental/ai_inter/` 下的全部代码提取为独立的 `feature:ai` 模块，实现复用性——其他模块（如 feature:debug）可直接依赖使用 AI 功能。

## 范围

### 迁移内容

| 目录/文件 | 说明 |
|---|---|
| `chat/` | 聊天功能（Route、ViewModel、UI State、components、data、export、highlight、markdown） |
| `data/` | 数据层（Repository、Database、DAOs、Models） |
| `network/` | 网络层（与 core:ai 重叠的删除，保留 feature 特有部分） |
| `viewmodel/` | ViewModel |
| `di/` | Hilt DI Module |
| `AiInterScreen.kt` | AI 主页面 |
| `AiInterSubScreens.kt` | AI 子页面 |
| `AiInterLogDetailRoute.kt` | 日志详情页 |
| `AiChatFlow.kt` | 聊天流处理 |

### 不迁移（保留在 core:ai）

| 代码 | 原因 |
|---|---|
| `AiInterApiClient` | 网络层已在 core:ai |
| `ToolSchemaJson` | 工具 schema 已在 core:ai |
| `StreamEvent` | 流式事件已在 core:ai |
| `AiChatToolHelper` | toolcall 已在 core:ai |
| `ToolCallBuffer` / `ToolCallRecord` | toolcall 已在 core:ai |
| `AiInterConfig` / `AiConfigProvider` | 配置已在 core:ai |

## 模块结构

```
feature/ai/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    └── java/com/nltimer/feature/ai/
        ├── navigation/
        │   ├── AiRoutes.kt          # AI 路由常量（从 NLtimerRoutes 迁移）
        │   └── AiNavGraph.kt        # 独立 NavGraph
        ├── di/
        │   └── AiModule.kt          # Hilt DI
        ├── data/
        │   ├── AiInterDatabase.kt
        │   ├── AiCallLogDao.kt
        │   ├── AiCallLogEntity.kt
        │   ├── AiCallLogModels.kt
        │   └── AiInterRepository.kt
        ├── chat/
        │   ├── AiAssistantChatRoute.kt
        │   ├── AiAssistantChatUiState.kt
        │   ├── AiAssistantChatViewModel.kt
        │   ├── StreamingState.kt
        │   ├── components/
        │   ├── data/
        │   ├── export/
        │   ├── highlight/
        │   └── markdown/
        ├── viewmodel/
        │   ├── AiInterViewModel.kt
        │   └── ChatMessage.kt
        ├── AiInterScreen.kt
        ├── AiInterSubScreens.kt
        ├── AiInterLogDetailRoute.kt
        ├── AiProviderConfigScreen.kt
        ├── AiToolsListScreen.kt
        ├── AiCallLogsScreen.kt
        ├── AiPromptConfigScreen.kt
        └── AiTestChatScreen.kt
```

## 依赖关系

```
app
 ├── feature:ai          ← 新模块
 ├── core:ai             ← 网络层、toolcall、config
 ├── core:tools
 ├── core:designsystem
 ├── core:data
 └── feature:home, stats, settings, ...

feature:ai
 ├── core:ai
 ├── core:tools
 ├── core:designsystem
 └── core:data (仅当需要访问 app 主数据库时)
```

## 关键变更

### 1. 路由常量迁移

从 `app/navigation/NLtimerRoutes.kt` 中提取 AI 相关常量到 `feature:ai/navigation/AiRoutes.kt`：

```kotlin
// feature:ai/navigation/AiRoutes.kt
object AiRoutes {
    const val AI_INTER = "ai_inter"
    const val AI_PROVIDER_CONFIG = "ai_provider_config"
    const val AI_TOOLS_LIST = "ai_tools_list"
    const val AI_CALL_LOGS = "ai_call_logs"
    const val AI_PROMPT_CONFIG = "ai_prompt_config"
    const val AI_TEST_CHAT = "ai_test_chat"
    const val AI_ASSISTANT_CHAT = "ai_assistant_chat"
    const val AI_CALL_LOG_DETAIL = "ai_call_log_detail"
    const val AI_CALL_LOG_DETAIL_PATTERN = "ai_call_log_detail/{logId}"

    fun aiCallLogDetail(logId: Long) = "ai_call_log_detail/$logId"
}
```

`NLtimerRoutes` 中保留对 `AiRoutes` 的引用或直接使用字符串常量，保持 PRIMARY_ROUTES 和 SETTINGS_FULLSCREEN_ROUTES 不变。

### 2. 独立 NavGraph

`feature:ai/navigation/AiNavGraph.kt` 定义所有 AI 相关路由，app 的 NLtimerNavHost 通过 `include` 调用：

```kotlin
// feature:ai/navigation/AiNavGraph.kt
fun NavGraphBuilder.aiNavGraph(navController: NavHostController) {
    composable(AiRoutes.AI_INTER) { AiInterRoute(navController) }
    composable(AiRoutes.AI_PROVIDER_CONFIG) { AiProviderConfigRoute() }
    // ... 其他路由
}
```

### 3. 数据库独立

`AiInterDatabase` 从 app 主数据库完全独立，feature:ai 自己管理 Room Database 实例和迁移。

### 4. 网络层统一

删除 `feature:ai/network/` 下与 `core:ai` 重复的文件（AiInterApiClient、ToolSchemaJson、StreamEvent、AiNetModels），统一使用 `core:ai` 的实现。

### 5. 配置层上移

用户要求配置优先放在 core 层。当前 `AiInterConfig` 和 `AiConfigProvider` 已在 `core:ai/config/` 中，无需额外迁移。`AiInterRepository` 实现 `AiConfigProvider` 接口，保持不变。

## 依赖处理

### 需要新增的依赖

feature:ai 模块需要依赖：
- `core:ai` — 网络层、toolcall、config
- `core:tools` — ToolRegistry、ToolDefinition 等
- `core:designsystem` — UI 组件（GroupCard、SettingsEntryCard 等）
- Room — 数据库
- Hilt — DI
- Navigation Compose — 导航
- Compose BOM + Material3 — UI

### 需要移除的依赖

- `com.nltimer.app.navigation.NLtimerRoutes` — 改用 feature:ai 自己的 AiRoutes

## 迁移步骤

1. **创建 feature:ai 模块** — build.gradle.kts、AndroidManifest.xml
2. **迁移路由常量** — 从 NLtimerRoutes 提取 AI 常量到 AiRoutes
3. **迁移数据层** — Database、DAOs、Entity、Repository
4. **迁移网络层** — 删除与 core:ai 重复的文件，保留 feature 特有部分
5. **迁移 ViewModel** — AiInterViewModel、AiAssistantChatViewModel
6. **迁移 UI** — 所有 Route 和 Screen 文件
7. **迁移 DI** — AiInterModule → AiModule
8. **创建 NavGraph** — AiNavGraph.kt
9. **更新 app 模块** — NLtimerNavHost 使用 include，删除旧代码
10. **更新 settings.gradle.kts** — 添加 feature:ai
11. **编译验证** — 确保无编译错误

## 验证标准

- [ ] `./gradlew :feature:ai:compileDebugKotlin --no-daemon` 通过
- [ ] `./gradlew :app:compileDebugKotlin --no-daemon` 通过
- [ ] 功能行为与迁移前一致
- [ ] 其他模块可直接依赖 feature:ai 使用 AI 功能

## 风险

| 风险 | 缓解 |
|---|---|
| Room 数据库迁移 | 使用 fallbackToDestructiveMigration 或手动编写迁移 |
| 导航参数传递 | 保持参数名称不变，避免路由解析失败 |
| Hilt 作用域 | 确保 SingletonComponent 作用域正确 |
