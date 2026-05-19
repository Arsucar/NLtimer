# PRD: 全项目彻夜无人值守优化

## 目标
对 NLtimer Android 项目进行一次全面的代码质量、性能、架构和 Kotlin/Compose 最佳实践优化。
所有修改必须是**安全的**——不改变业务逻辑、不破坏现有功能、不引入新 bug。

## 范围

### 高优先级（直接影响用户体验 / 代码健康度）

#### C1. 消除 AI 模块大规模代码重复
- **问题**: `AiInterViewModel` (533 行) 与 `AiAssistantChatViewModel` (539 行) 存在大面积复制粘贴：
  - `TOOLS_SYSTEM_PROMPT` 常量完全相同（~30 行 x2）
  - `ToolCallBuffer` 内部类完全相同
  - `executeToolCall()` 方法几乎完全相同
  - `parseToolArguments()` 方法完全相同
  - `serializeToolCalls()` 方法完全相同
  - `sendMessage()` 中的流式处理循环结构高度相似
- **修复**: 提取共享逻辑到 `AiChatShared` 或类似工具类/基类中
- **文件**:
  - `app/.../ai_inter/viewmodel/AiInterViewModel.kt`
  - `app/.../ai_inter/chat/AiAssistantChatViewModel.kt`

#### C2. 修复 BehaviorManagementViewModel 使用 AndroidViewModel
- **问题**: `BehaviorManagementViewModel` 继承 `AndroidViewModel` 仅为了获取 `Application` 访问 `ContentResolver`，这破坏了可测试性和 Hilt DI 原则
- **修复**: 注入 `@ApplicationContext context: Context` 替代继承 `AndroidViewModel`
- **文件**: `feature/behavior_management/.../viewmodel/BehaviorManagementViewModel.kt`

#### C3. ThemeSettingsScreen 过大 (875 行 → 目标 < 500 行)
- **问题**: 单文件包含所有主题设置 UI，大量重复的 section 模式
- **修复**: 将每个设置 section（调色板/字体/形状/边框/透明度/表达力/顶栏/底栏）提取为独立 composable
- **文件**: `feature/settings/.../ui/ThemeSettingsScreen.kt`

#### C4. SettingsPrefsImpl 中的重复 enum 解析模式
- **问题**: 22 处相同的 `try { EnumType.valueOf(...) } catch (_: IllegalArgumentException) { default }` 模式
- **修复**: 提取通用 inline `reified` 扩展函数 `safeValueOf(name: String, default: T)`
- **文件**: `core/data/.../SettingsPrefsImpl.kt`

#### C5. 修复 collectAsState 使用（应用生命周期感知收集）
- **问题**: `AiInterSubScreens.kt` 和 `AiAssistantChatRoute.kt` 使用 `collectAsState` 而非 `collectAsStateWithLifecycle`，导致后台继续处理
- **修复**: 替换为 `collectAsStateWithLifecycle()`
- **文件**:
  - `app/.../ai_inter/AiInterSubScreens.kt`
  - `app/.../ai_inter/chat/AiAssistantChatRoute.kt`

#### C6. 修复 SharingStarted.Lazily 误用
- **问题**: `BehaviorManagementViewModel` 的 7 个 stateIn 使用 `SharingStarted.Lazily`——一旦启动永不停止，即使页面离开
- **修复**: 改为 `SharingStarted.WhileSubscribed(5000)` 与其他 ViewModel 保持一致
- **文件**: `feature/behavior_management/.../viewmodel/BehaviorManagementViewModel.kt`

### 中优先级（代码质量 / 可维护性）

#### C7. AppTopAppBar 重复代码 (365 行)
- **问题**: 该文件包含两套几乎相同的 TopAppBar 实现（pinned 和 scrolling 模式），大量代码复制
- **修复**: 合并两套实现，通过参数控制差异部分
- **文件**: `app/.../component/AppTopAppBar.kt`

#### C8. 硬编码颜色值
- **问题**: `DualTimePickerComponent.kt` 使用 `Color(0xFF0A1034)` 等硬编码颜色
- **修复**: 使用 `MaterialTheme.colorScheme` 或在 theme 中定义 semantic color
- **文件**: `core/behaviorui/.../sheet/DualTimePickerComponent.kt` (line 37)

#### C9. SimpleDateFormat 替换为 java.time API
- **问题**: 3 处使用 `SimpleDateFormat`，在现代 Kotlin 中应使用 `java.time.format.DateTimeFormatter`
- **文件**:
  - `app/.../ai_inter/chat/export/ConversationExporter.kt`
  - `app/.../ai_inter/AiInterSubScreens.kt`
  - `app/.../ai_inter/AiInterLogDetailRoute.kt`

#### C10. 清理通配符 import
- **问题**: AI 模块约 30 处使用 `import xxx.*` 通配符导入
- **修复**: 替换为显式 import（仅限 `app/.../experimental/ai_inter/` 目录）
- **文件**: AI 模块下的所有 Composable 组件文件

#### C11. 拖拽排序逻辑重复
- **问题**: `TagManagementScreen.kt`、`ActivityManagementScreen.kt` 和 `CategoryPickerDialog.kt` 三处包含几乎相同的拖拽排序逻辑（mutableStateListOf + mutableStateMapOf + shiftOffsets + 动画）
- **修复**: 提取为共享的可复用拖拽排序组件或 modifier
- **文件**:
  - `feature/tag_management/.../ui/TagManagementScreen.kt`
  - `feature/management_activities/.../ui/ActivityManagementScreen.kt`
  - `core/behaviorui/.../sheet/CategoryPickerDialog.kt`

#### C12. HomeViewModel 初始化中多个独立的 viewModelScope.launch
- **问题**: `init` 块中有 4 个独立的 `viewModelScope.launch`，每个都在 collect，但它们之间有依赖关系（activities 和 tags 需要在 behaviors 之前加载）
- **修复**: 考虑使用 `combine` + 单一 `stateIn` 统一管理，或在 `loadHomeBehaviors` 中确保依赖数据已就绪
- **文件**: `feature/home/.../viewmodel/HomeViewModel.kt`

#### C13. 缺少 Modifier 参数的 Composable 函数
- **问题**: 大量私有 composable 函数缺少 `modifier: Modifier = Modifier` 参数，导致调用方无法自定义布局
- **修复**: 为可复用的 composable 添加 modifier 参数
- **文件**: 多个 `core/designsystem/` 和 `feature/` 下的文件

#### C14. 修复 displayColorConfigFlow 中的全限定类名
- **问题**: `SettingsPrefsImpl` 中使用了 `com.nltimer.core.data.model.DisplayColorConfig` 和 `com.nltimer.core.designsystem.theme.DisplayColorMode` 全限定名
- **修复**: 添加 import 语句
- **文件**: `core/data/.../SettingsPrefsImpl.kt` (lines 260-263, 267)

### 低优先级（优化 / 最佳实践）

#### C15. TODO 标记处理
- **问题**: `BehaviorRepositoryImpl.kt` line 167 有未完成的 TODO
- **修复**: 实现或移除 TODO
- **文件**: `core/data/.../repository/impl/BehaviorRepositoryImpl.kt`

#### C16. exportToJson 和 importFromJson 中异常被吞掉
- **问题**: `BehaviorManagementViewModel` 的 `exportToJson` 和 `importFromJson` 中 `catch (_: Exception)` 完全忽略错误
- **修复**: 至少记录日志或向用户展示错误
- **文件**: `feature/behavior_management/.../viewmodel/BehaviorManagementViewModel.kt`

#### C17. detekt 配置检查
- **确认**: 项目已配置 Detekt，需验证其规则覆盖度（LongParameterList、LongMethod 等是否有阈值限制）
- **文件**: 根目录 `detekt-config.yml` 或 `detekt.yml`

## 不在范围内

- 不修改业务逻辑
- 不重构数据模型
- 不升级依赖版本
- 不修改 build.gradle 依赖配置（除非发现未使用的依赖）
- debug/preview 文件不做优化（仅用于开发调试）
- `docs/ai generate/` 目录下的文件不做处理
- `core/designsystem/src/.../icon/MaterialIconCatalog.kt`（自动生成的图标目录）不做拆分

## 验收标准

1. 所有现有测试通过 (`./gradlew test --no-daemon`)
2. 编译成功 (`./gradlew :app:compileDebugKotlin --no-daemon`)
3. 无新增 lint/detekt 警告
4. 代码行数净减少（消除重复）
5. 所有 `// TODO` 有明确处理或记录为后续任务

## 实现顺序建议

1. C4 (enum 解析工具) → 无风险基础设施
2. C6 (SharingStarted 修复) → 一行改动，高收益
3. C5 (collectAsState 修复) → 一行改动，高收益
4. C2 (AndroidViewModel 替换) → DI 层改进
5. C14 (全限定名清理) → 简单清理
6. C15 (TODO 处理) → 简单清理
7. C16 (异常处理) → 简单改进
8. C10 (通配符 import) → IDE 可自动完成
9. C9 (SimpleDateFormat) → 简单替换
10. C8 (硬编码颜色) → 需要视觉验证
11. C11 (拖拽排序去重) → 中等难度
12. C7 (TopAppBar 去重) → 中等难度
13. C3 (ThemeSettingsScreen 拆分) → 大文件拆分
14. C1 (AI 模块去重) → 最大改动，放最后
15. C12 (HomeViewModel 重构) → 可选
16. C13 (Modifier 参数) → 可选，低优先级
