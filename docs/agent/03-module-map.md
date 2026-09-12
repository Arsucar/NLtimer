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
| ArchiveGroup | `core/data/.../model/ArchiveGroup.kt` | 归档区分组聚合 |
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
| ArchiveItemDetailDialog | `core/designsystem/.../component/` | 归档条目详情（日期+感想+恢复） |
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
| StartBehaviorTool | `core/tools/.../timing/` | 开始行为工具 |
| EndBehaviorTool | `core/tools/.../timing/` | 结束行为工具 |
| RecordBehaviorTool | `core/tools/.../timing/` | 补录行为工具 |
| QueryCurrentBehaviorTool | `core/tools/.../timing/` | 查询当前行为（含活动名/标签/时长） |
| ListBehaviorsTool | `core/tools/.../timing/` | 按时间范围查行为列表 |
| ListActivitiesTool | `core/tools/.../timing/` | 列出活动（含分组名/标签） |
| GetDailySummaryTool | `core/tools/.../timing/` | 日统计汇总 |
| GetWeeklySummaryTool | `core/tools/.../timing/` | 周统计汇总 |
| GetTimeRangeSummaryTool | `core/tools/.../timing/` | 自定义区间统计 |
| GetBehaviorDetailTool | `core/tools/.../timing/` | 单条行为详情 |
| CreateGoalTool | `core/tools/.../timing/` | 创建 PENDING 目标 |
| ListGoalsTool | `core/tools/.../timing/` | 列出待办目标 |
| ActivateGoalTool | `core/tools/.../timing/` | 激活目标开始计时 |
| DeleteGoalTool | `core/tools/.../timing/` | 取消目标 |
| ReorderGoalsTool | `core/tools/.../timing/` | 重排目标顺序 |
| UpdateBehaviorTool | `core/tools/.../timing/` | 修改已有行为 |
| DeleteBehaviorTool | `core/tools/.../timing/` | 删除行为 |
| NoteMatcher | `core/tools/.../match/` | 备注匹配工具 |
| SearchIconsTool | `core/tools/.../timing/` | 单关键词图标搜索（含同义词回退） |
| BatchSearchIconsTool | `core/tools/.../timing/` | 批量关键词图标搜索 |
| IconSearchEngine | `core/tools/.../timing/` | 图标搜索引擎（共享搜索+同义词逻辑） |
| BatchCreateActivitiesTool | `core/tools/.../library/` | 批量创建活动（支持 autoIcon） |
| BatchCreateTagsTool | `core/tools/.../library/` | 批量创建标签（支持 autoIcon） |
| BulkUpdateActivitiesTool | `core/tools/.../library/` | 批量修改活动属性 |
| BulkUpdateTagsTool | `core/tools/.../library/` | 批量修改标签属性 |

## core:ai

| 关键类 | 路径 | 职责 |
|--------|------|------|
| AiInterApiClient | `core/ai/.../network/AiInterApiClient.kt` | AI 流式请求客户端 |
| AiInterConfig | `core/ai/.../config/AiInterConfig.kt` | AI 配置模型 |
| AiChatToolHelper | `core/ai/.../toolcall/AiChatToolHelper.kt` | Tool Call 辅助 |

## feature:ai

| 关键类 | 路径 | 职责 |
|--------|------|------|
| AiRoutes | `feature/ai/.../navigation/AiRoutes.kt` | AI 路由常量 |
| AiNavGraph | `feature/ai/.../navigation/AiNavGraph.kt` | AI 导航图 |
| AiInterScreen | `feature/ai/.../AiInterScreen.kt` | AI Inter 主页 |
| AiAssistantChatRoute | `feature/ai/.../chat/AiAssistantChatRoute.kt` | 助手聊天路由 |

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
| EventSheet | `feature/home/.../ui/components/event/` | 事件 BottomSheet（列表/表单双页态） |
| EventFormPage | `feature/home/.../ui/components/event/` | 事件录入表单（归属/模板切换/快速·完整模式） |
| StarRatingBar | `feature/home/.../ui/components/event/` | 星级评分组件（1~5，重按清除） |
| EventSummaryRow | `feature/home/.../ui/components/moment/` | ACTIVE 卡片事件摘要行（📝 N 笔 · 最新事件） |

## feature:settings

| 关键类 | 路径 | 职责 |
|--------|------|------|
## feature:settings

| 关键类 | 路径 | 职责 |
|--------|------|------|
| SettingsScreen | `feature/settings/.../ui/` | 设置主页（含「打点模板」入口） |
| ThemeSettingsScreen | `feature/settings/.../ui/` | 主题设置 |
| DialogConfigScreen | `feature/settings/.../ui/` | 弹窗配置 |
| DataManagementScreen | `feature/settings/.../ui/` | 数据管理 |
| HomeLayoutConfigScreen | `feature/settings/.../ui/` | 首页布局配置 |
| EventTemplateListScreen | `feature/settings/.../ui/eventtemplate/` | 打点模板列表页（事件记录器） |
| EventTemplateEditScreen | `feature/settings/.../ui/eventtemplate/` | 模板编辑页（字段/类型/选项/排序） |
| EventTemplateViewModel | `feature/settings/.../ui/eventtemplate/` | 模板管理状态 |

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
| TagManagementViewModel | `feature/tag_management/.../viewmodel/` | 标签状态管理（含绑定打点模板/关联事件流） |
| TagEventsDialog | `feature/tag_management/.../dialogs/` | 标签关联事件弹层（只读列表+删除） |
| TagArchiveRoute | `feature/tag_management/.../ui/` | 标签归档区路由 |
| TagArchiveScreen | `feature/tag_management/.../ui/` | 标签归档区 UI |
| TagArchiveViewModel | `feature/tag_management/.../viewmodel/` | 标签归档区状态 |

## feature:management_activities

| 关键类 | 路径 | 职责 |
|--------|------|------|
| ActivityManagementRoute | `feature/management_activities/.../ui/` | 活动管理路由 |
| ActivityManagementScreen | `feature/management_activities/.../ui/` | 活动管理页 |
| ActivityManagementViewModel | `feature/management_activities/.../viewmodel/` | 活动状态管理 |
| ActivityArchiveRoute | `feature/management_activities/.../ui/` | 活动归档区路由 |
| ActivityArchiveScreen | `feature/management_activities/.../ui/` | 活动归档区 UI |
| ActivityArchiveViewModel | `feature/management_activities/.../viewmodel/` | 活动归档区状态 |
| ActivityDetailSheet | `feature/management_activities/.../components/` | 活动详情弹层（含「复盘事件」分区） |

## feature:stats

| 关键类 | 路径 | 职责 |
|--------|------|------|
| StatsScreen | `feature/stats/.../ui/` | 统计页：LazyVerticalGrid 面板仪表盘（StatsPanelType 可编辑拖拽） |
| EventsPanelContent | `feature/stats/.../ui/component/` | 「复盘事件」面板（EVENTS）：混排⇄聚焦、卡片⇄表格、事件列表 |
| EventFilterBar | `feature/stats/.../ui/component/` | 事件动态筛选（选项多选/数值星级 RangeSlider/文本 LIKE） |
| EventsPanelModel | `feature/stats/.../model/` | 事件面板 UiState + 客户端过滤纯函数 |
| StatsViewModel | `feature/stats/.../viewmodel/` | 仪表盘 + 事件面板流（含持久化） |

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
