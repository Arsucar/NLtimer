# 模块映射表

## app

| 关键类 | 路径 | 职责 |
|--------|------|------|
| MainActivity | `app/.../MainActivity.kt` | 单 Activity 入口 |
| NLtimerApplication | `app/.../NLtimerApplication.kt` | Hilt Application |
| NLtimerApp | `app/.../NLtimerApp.kt` | 根 Composable |
| NLtimerScaffold | `app/.../NLtimerScaffold.kt` | Drawer+TopBar+BottomNav 骨架 |
| NLtimerNavHost | `app/.../navigation/NLtimerNavHost.kt` | 导航图注册 |
| NLtimerRoutes | `app/.../navigation/NLtimerRoutes.kt` | 路由常量 |
| DrawerViewModel | `app/.../viewmodel/DrawerViewModel.kt` | 抽屉状态 |
| AiInterViewModel | `app/.../experimental/ai_inter/viewmodel/` | AI 聊天（实验性） |

## core:data

| 关键类 | 路径 | 职责 |
|--------|------|------|
| NLtimerDatabase | `core/data/.../database/NLtimerDatabase.kt` | Room DB 定义 |
| ActivityDao | `core/data/.../database/dao/ActivityDao.kt` | 活动 CRUD |
| ActivityGroupDao | `core/data/.../database/dao/ActivityGroupDao.kt` | 分组 CRUD |
| TagDao | `core/data/.../database/dao/TagDao.kt` | 标签 CRUD |
| BehaviorDao | `core/data/.../database/dao/BehaviorDao.kt` | 行为 CRUD |
| ActivityEntity | `core/data/.../database/entity/ActivityEntity.kt` | 活动表 |
| BehaviorEntity | `core/data/.../database/entity/BehaviorEntity.kt` | 行为表 |
| TagEntity | `core/data/.../database/entity/TagEntity.kt` | 标签表 |
| Behavior | `core/data/.../model/Behavior.kt` | 行为领域模型 |
| Activity | `core/data/.../model/Activity.kt` | 活动领域模型 |
| AddBehaviorUseCase | `core/data/.../usecase/AddBehaviorUseCase.kt` | 添加行为逻辑 |
| ExportDataUseCase | `core/data/.../usecase/ExportDataUseCase.kt` | 数据导出 |
| TimeFormatUtils | `core/data/.../util/TimeFormatUtils.kt` | 时间格式化 |
| SettingsPrefs | `core/data/.../` | DataStore 偏好 |

## core:designsystem

| 关键类 | 路径 | 职责 |
|--------|------|------|
| NLtimerTheme | `core/designsystem/.../theme/NLtimerTheme.kt` | 主题入口 |
| AppTheme | `core/designsystem/.../theme/AppTheme.kt` | 主题配置 |
| PaletteStyle | `core/designsystem/.../theme/PaletteStyle.kt` | 调色板风格 |
| FormSpec | `core/designsystem/.../form/FormSpec.kt` | 表单定义 |
| IconRenderer | `core/designsystem/.../icon/IconRenderer.kt` | 图标渲染 |
| GenericFormDialog | `core/designsystem/.../form/` | 通用表单对话框 |
| ConfirmDialog | `core/designsystem/.../component/` | 确认对话框 |
| GroupCard | `core/designsystem/.../component/` | 分组卡片 |

## core:behaviorui

| 关键类 | 路径 | 职责 |
|--------|------|------|
| AddBehaviorSheet | `core/behaviorui/.../AddBehaviorSheet.kt` | 行为添加面板 |
| WheelPicker | `core/behaviorui/.../` | 滚轮选择器 |
| ActivityPicker | `core/behaviorui/.../` | 活动选择器 |
| TagPicker | `core/behaviorui/.../` | 标签选择器 |
| DualTimePickerComponent | `core/behaviorui/.../` | 双时间选择器 |

## core:tools

| 关键类 | 路径 | 职责 |
|--------|------|------|
| ToolRegistry | `core/tools/.../ToolRegistry.kt` | 工具注册中心 |
| ToolDefinition | `core/tools/.../ToolDefinition.kt` | 工具定义接口 |
| StartBehaviorTool | `core/tools/.../` | 开始行为工具 |
| NoteMatcher | `core/tools/.../` | 备注匹配工具 |

## feature:home

| 关键类 | 路径 | 职责 |
|--------|------|------|
| HomeScreen | `feature/home/.../ui/` | 首页 UI |
| HomeViewModel | `feature/home/.../viewmodel/` | 首页状态管理 |
| GridCell | `feature/home/.../ui/` | 网格单元格 |
| TimeAxisGrid | `feature/home/.../ui/` | 网格布局 |
| TimelineReverseView | `feature/home/.../ui/` | 时间线布局 |
| MomentView | `feature/home/.../ui/` | 此刻布局 |
| BehaviorLogView | `feature/home/.../ui/` | 日志布局 |

## feature:settings

| 关键类 | 路径 | 职责 |
|--------|------|------|
| SettingsScreen | `feature/settings/.../ui/` | 设置主页 |
| ThemeSettingsScreen | `feature/settings/.../ui/` | 主题设置 |
| DialogConfigScreen | `feature/settings/.../ui/` | 弹窗配置 |
| DataManagementScreen | `feature/settings/.../ui/` | 数据管理 |
| HomeLayoutConfigScreen | `feature/settings/.../ui/` | 首页布局配置 |

## feature:behavior_management

| 关键类 | 路径 | 职责 |
|--------|------|------|
| BehaviorManagementScreen | `feature/behavior_management/.../ui/` | 行为管理页 |
| JsonExporter | `feature/behavior_management/.../export/` | JSON 导出 |
| JsonImporter | `feature/behavior_management/.../` | JSON 导入 |

## feature:categories

| 关键类 | 路径 | 职责 |
|--------|------|------|
| CategoriesRoute | `feature/categories/.../ui/` | 分类页路由 |
| CategoriesScreen | `feature/categories/.../ui/` | 分类页 UI |
| CategoriesViewModel | `feature/categories/.../viewmodel/` | 分类状态管理 |

## feature:tag_management

| 关键类 | 路径 | 职责 |
|--------|------|------|
| TagManagementRoute | `feature/tag_management/.../ui/` | 标签管理路由 |
| TagManagementScreen | `feature/tag_management/.../ui/` | 标签管理页 |
| TagManagementViewModel | `feature/tag_management/.../viewmodel/` | 标签状态管理 |

## feature:management_activities

| 关键类 | 路径 | 职责 |
|--------|------|------|
| ActivityManagementRoute | `feature/management_activities/.../ui/` | 活动管理路由 |
| ActivityManagementScreen | `feature/management_activities/.../ui/` | 活动管理页 |
| ActivityManagementViewModel | `feature/management_activities/.../viewmodel/` | 活动状态管理 |

## feature:stats

| 关键类 | 路径 | 职责 |
|--------|------|------|
| StatsScreen | `feature/stats/.../ui/` | 统计页（占位） |

## feature:sub

| 关键类 | 路径 | 职责 |
|--------|------|------|
| SubScreen | `feature/sub/.../ui/` | 订阅页（占位） |

## feature:debug

| 关键类 | 路径 | 职责 |
|--------|------|------|
| DebugRoute | `feature/debug/.../` | Debug 入口 |
| DebugPage | `feature/debug/.../ui/` | Debug 主页 |
| ToolConsoleViewModel | `feature/debug/.../tools/` | 工具控制台 |

## core:debugui

| 关键类 | 路径 | 职责 |
|--------|------|------|
| FieldDetailDialog | `core/debugui/.../` | 字段详情对话框 |
| FieldInfo | `core/debugui/.../` | 字段信息模型 |
