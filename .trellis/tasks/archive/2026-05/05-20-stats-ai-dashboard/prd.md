# 统计页面：AI 可定制仪表板

> **状态：Phase 1-3 已完成，Phase 4 待后续迭代**
>
> 完成提交：`6df1a2fe` (主体实现) + `741ca466` (review 修复)

## 需求概述

将当前空白的统计页面（`feature/stats`）建设为 **固定数据架构 + AI 可定制仪表板** 的完整统计中心。用户可以通过 AI 助手自定义筛选条件、展示样式和布局，保存最适合自己的回顾方式。

## 设计理念

- **固定层**：数据查询引擎（时间范围、分组维度、聚合计算）是确定的
- **可变层**：筛选条件组合、图表/卡片布局、展示样式，序列化为 JSON 配置，交给用户或 AI 调整
- **渐进式交付**：先完成基础统计，再叠加 AI 定制能力

## 详细需求

### Phase 1：数据基础设施（core:data 层） ✅ 已完成

#### 1.1 统计领域模型

新建 `core/data/model/` 下的统计模型：

```kotlin
data class StatsResult(
    val timeRange: StatsTimeRange,
    val totalMinutes: Int,
    val totalHours: String,
    val completedCount: Int,
    val activeCount: Int,
    val pendingCount: Int,
    val plannedCount: Int,
    val completionRate: String,
    val totalEstimatedMinutes: Int,
    val totalActualMinutes: Int,
    val planAdherenceRate: String?,
    val activityStats: List<ActivityStat>,
    val dailyBreakdown: List<DailyBreakdown>,
    val tagStats: List<TagStat>,
)

data class ActivityStat(
    val activityId: Long,
    val activityName: String,
    val iconKey: String?,
    val durationMinutes: Int,
    val count: Int,
    val plannedCount: Int,
    val avgAchievement: String?,
)

data class DailyBreakdown(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val totalMinutes: Int,
    val activityBreakdown: List<ActivityDailyItem>,
)

data class ActivityDailyItem(
    val activityId: Long,
    val activityName: String,
    val iconKey: String?,
    val durationMinutes: Int,
)

data class TagStat(
    val tagId: Long,
    val tagName: String,
    val iconKey: String?,
    val durationMinutes: Int,
    val count: Int,
)
```

#### 1.2 StatsQueryUseCase

新建 `core/data/usecase/StatsQueryUseCase.kt`：

- 复用 `BehaviorRepository.getBehaviorsWithDetailsByTimeRangeSync()` 做数据查询
- 封装通用聚合逻辑：按活动/标签/日期分组，计算时长、次数、完成率、计划达成率等
- 抽取 `GetDailySummaryTool` / `GetWeeklySummaryTool` / `GetTimeRangeSummaryTool` 中的共享聚合逻辑，消除重复

### Phase 2：仪表板配置模型（core:data 层） ✅ 已完成

#### 2.1 StatsDashboardConfig

参照 `HomeLayoutConfig` 模式，新建配置模型：

```kotlin
data class StatsDashboardConfig(
    val panels: List<StatsPanelConfig>,
    val defaultTimeRange: StatsTimeRange = StatsTimeRange(StatsTimeRangeType.WEEK),
)

data class StatsPanelConfig(
    val id: String,
    val type: StatsPanelType,
    val title: String,
    val timeRange: StatsTimeRange? = null,
    val filters: StatsFilters = StatsFilters(),
    val displayOptions: StatsDisplayOpts = StatsDisplayOpts(),
)

enum class StatsPanelType {
    PIE_CHART,         // 饼图：按活动时长占比
    BAR_CHART,         // 柱状图：按天/周/活动
    TIMELINE_HEATMAP,  // 热力图：24h x 7天
    SUMMARY_CARD,      // 汇总卡片：总时长/完成率/最活跃活动
    TREND_LINE,        // 趋势线：一段时间内某指标变化
    RANKING_LIST,      // 排行榜：按时长/次数排序
    COMPARISON,        // 对比：本周 vs 上周
    AI_INSIGHT,        // AI 洞察文本块
}

data class StatsFilters(
    val activityIds: List<Long>? = null,
    val tagIds: List<Long>? = null,
    val groupIds: List<Long>? = null,
    val statusFilter: Set<BehaviorNature>? = null,
    val wasPlanned: Boolean? = null,
)

enum class StatsTimeRangeType { DAY, WEEK, MONTH, CUSTOM }

data class StatsTimeRange(
    val type: StatsTimeRangeType,
    val offset: Int = 0,
    val customStart: Long? = null,
    val customEnd: Long? = null,
)

data class StatsDisplayOpts(
    val sortBy: StatsSortField = StatsSortField.DURATION,
    val sortOrder: SortOrder = SortOrder.DESC,
    val maxItems: Int = 10,
    val showIcon: Boolean = true,
    val showPercentage: Boolean = true,
    val colorScheme: StatsColorScheme = StatsColorScheme.ACTIVITY_COLOR,
)

enum class StatsSortField { DURATION, COUNT, NAME, ACHIEVEMENT }
enum class SortOrder { ASC, DESC }
enum class StatsColorScheme { ACTIVITY_COLOR, AUTO, MONO }
```

#### 2.2 SettingsPrefs 扩展

在 `SettingsPrefs` 接口中新增：

```kotlin
fun getStatsDashboardConfigFlow(): Flow<StatsDashboardConfig>
suspend fun updateStatsDashboardConfig(config: StatsDashboardConfig)
```

`SettingsPrefsImpl` 中新增 DataStore key + JSON 序列化（使用 Kotlinx Serialization）。

### Phase 3：固定面板实现（feature:stats 层） ✅ 已完成

#### 3.1 StatsViewModel

新建 `StatsViewModel`（`feature/stats/viewmodel/`）：

- 注入 `StatsQueryUseCase` + `SettingsPrefs`
- `UiState` 包含：当前 dashboard config + 各面板已加载的数据
- 根据每个 panel 的 timeRange + filters 查询对应数据

#### 3.2 StatsRoute + StatsScreen 重构

- `StatsRoute` 注入 ViewModel，收集 state
- `StatsScreen` 布局：
  - 顶部：时间范围切换 Chip（今日/本周/本月/自定义）
  - 主体：LazyColumn 渲染面板列表，每个面板根据 `StatsPanelType` 分发到对应 Composable
  - 底部 FAB：「编辑仪表板」入口

#### 3.3 面板 Composable

实际实现的面板（LazyVerticalGrid 4列网格布局）：

| 面板 | 文件 | 说明 |
|------|------|------|
| METRIC_CARD | `MetricCard.kt` | 独立指标卡片（总时长/完成数/活跃数/待办数），colSpan=2 |
| BAR_CHART | `BarChartCard.kt` | Canvas 柱状图，按天展示每日总时长 |
| PIE_CHART | `CategoryShareCard.kt` | Canvas 环形饼图，按活动时长占比 |
| RANKING_LIST | `ActivityRankSection.kt` | 活动排行榜 + 时长条 |
| COMPARISON | `ComparisonCard.kt` | 本周 vs 上周对比 |
| TREND_LINE | `TrendCard.kt` | 趋势折线图（已创建，待数据接入） |

辅助组件：`PressableStatsCard.kt`（可按压卡片壳）、`StatsGridContainer.kt`（拖拽排序网格容器）。

#### 3.4 面板编辑模式 ✅ 已完成

- 编辑模式下：上下移动、删除、添加面板、恢复默认
- 添加面板弹窗：显示所有面板类型，未实现的标注"即将支持"
- 导航栏安全间距（navigationBarsPadding）
- 统计页隐藏外层 Scaffold 顶栏，使用自身全屏布局

### Phase 4：AI 定制能力 🔲 待后续迭代

#### 4.1 ConfigureStatsDashboardTool

新建 `core/tools/stats/ConfigureStatsDashboardTool.kt`：

- 注册到 `ToolRegistry`
- 参数：用户自然语言描述的偏好（由 AI 解析为结构化参数）
- 功能：生成/修改 `StatsDashboardConfig` 并写入 DataStore
- 用户在 AI 聊天中可以说「帮我加一个面板看看这周每天的学习时长对比」

#### 4.2 预设模板

提供几个预设仪表板模板（JSON 资源文件或硬编码）：

| 模板 | 面板组成 |
|------|---------|
| 效率概览 | 汇总卡片 + 饼图 + 排行榜 |
| 学习回顾 | 柱状图 + 趋势线 + 排行榜 |
| 工作周报 | 汇总卡片 + 柱状图 + AI 洞察 |

用户可选择模板后让 AI 微调。

#### 4.3 统计页内嵌 AI 入口（可选增强）

- 统计页右上角 AI 图标按钮
- 点击弹出简短对话浮层
- 用户描述想要的视图 → AI 返回 `StatsPanelConfig` → 预览 → 确认应用
- 复用 `AiInterApiClient` 的 SSE 流式能力

## 涉及文件

### 新建（已创建）
- `core/data/src/main/java/.../model/StatsModels.kt` — 统计领域模型
- `core/data/src/main/java/.../model/StatsDashboardConfig.kt` — 仪表板配置模型（含 StatsPanelType/METRIC_CARD/colSpan）
- `core/data/src/main/java/.../usecase/StatsQueryUseCase.kt` — 统计查询用例
- `feature/stats/src/main/java/.../viewmodel/StatsViewModel.kt` — 统计页 ViewModel
- `feature/stats/src/main/java/.../model/StatsUiState.kt` — UI 状态模型
- `feature/stats/src/main/java/.../ui/StatsRoute.kt` — 路由
- `feature/stats/src/main/java/.../ui/StatsScreen.kt` — 网格布局 + 编辑模式
- `feature/stats/src/main/java/.../ui/component/MetricCard.kt` — 指标卡片
- `feature/stats/src/main/java/.../ui/component/BarChartCard.kt` — 柱状图
- `feature/stats/src/main/java/.../ui/component/CategoryShareCard.kt` — 饼图
- `feature/stats/src/main/java/.../ui/component/ActivityRankSection.kt` — 排行榜
- `feature/stats/src/main/java/.../ui/component/ComparisonCard.kt` — 对比卡片
- `feature/stats/src/main/java/.../ui/component/TrendCard.kt` — 趋势图
- `feature/stats/src/main/java/.../ui/component/PressableStatsCard.kt` — 通用卡片壳
- `feature/stats/src/main/java/.../ui/component/StatsGridContainer.kt` — 拖拽排序网格

### 修改（已修改）
- `core/data/SettingsPrefs.kt` — 新增 getStatsDashboardConfigFlow / updateStatsDashboardConfig
- `core/data/SettingsPrefsImpl.kt` — DataStore JSON 持久化（ignoreUnknownKeys）
- `app/NLtimerScaffold.kt` — 统计页隐藏顶栏 + isHomePage/isAiAssistantChat 排除
- `app/component/AppTopAppBar.kt` — titleStyle 缩进修正

### 待新建（Phase 4）
- `core/tools/src/main/java/.../stats/ConfigureStatsDashboardTool.kt` — AI 配置工具

## 实施优先级

| 顺序 | 内容 | 依赖 | 状态 |
|------|------|------|------|
| 1 | StatsModels + StatsQueryUseCase | 无 | ✅ |
| 2 | StatsDashboardConfig + DataStore 持久化 | 无 | ✅ |
| 3 | StatsViewModel + 固定面板（指标卡片、饼图、排行榜、柱状图、对比） | 1, 2 | ✅ |
| 4 | 面板排序/增删/编辑 UI | 3 | ✅ |
| 5 | ConfigureStatsDashboardTool（AI 驱动配置） | 2 | 🔲 |
| 6 | 预设模板 + 统计页 AI 入口 | 5 | 🔲 |
| 7 | 高级图表（热力图、趋势线数据接入） | 3 | 🔲 TrendCard 已建，数据待接入 |

## 验收标准

### Phase 1-3 基础版 ✅
1. ✅ 统计页不再显示空白占位，展示 5+ 个面板（指标卡片×4 + 饼图 + 排行榜 + 柱状图 + 对比）
2. ✅ 时间范围切换（今日/本周/本月）正常工作
3. ✅ 面板数据准确（通过 StatsQueryUseCase 聚合查询）
4. ✅ 仪表板配置持久化（DataStore JSON），重启后恢复
5. ✅ 面板可编辑（上下移动、删除、添加面板、恢复默认）

### Phase 4 AI 定制 🔲
6. 🔲 AI 聊天中可以通过自然语言修改统计页配置
7. 🔲 预设模板可一键应用
8. 🔲 用户手动编辑和 AI 编辑的配置共存无冲突
