# 标签活动联动增强与UI修复

## Goal

增强标签与活动之间的双向联动能力，修复新增标签弹窗的默认图标和分类问题，改进抽屉栏和AI对话界面的交互体验。

## What I already know

* **标签-活动绑定**：已有 `ActivityTagBindingEntity` 和相关 DAO/Repository 支持 M:N 关系
* **新增标签弹窗**：`ActivityFormSpecs.createTag` 中 icon 默认为 emoji `🏷️`，应改为 hi 库图标 `hi:Tag01`
* **行为弹窗中的新增标签**：`SheetPickerSection.kt` 传 `categories = emptyList()`，导致无法选择分类
* **抽屉栏总用时**：`AppDrawer.kt` 中 `TotalDurationCircle` 使用 `ElevatedCard`，需改为强调色背景
* **AI对话侧滑**：`AiAssistantChatRoute.kt` 使用 `ModalNavigationDrawer` + `ChatDrawer`（历史对话），需改为调用主抽屉栏
* **StartBehaviorTool** 已实现 tag→activity 绑定查询逻辑：`tagRepository.getActivityIdsForTag()`
* **NoteMatcher** 已实现备注自动扫描匹配活动和标签

## Requirements

### R1: 新增标签弹窗图标默认值
- 新增标签时，图标默认值从 emoji `🏷️` 改为 hi 图标库的 `hi:Tag01`

### R2: 修复行为弹窗中新增标签无法读取分类
- 在 `SheetPickerSection` 中，AddTagDialog 的 `categories` 参数从 `emptyList()` 改为传入实际的标签分类列表
- 需要从 AddBehaviorSheet 向下传递 tag 分类数据

### R3: 抽屉栏总用时改为强调色样式
- 将 `TotalDurationCircle` 的 `ElevatedCard` 改为使用主题强调色背景 + 对应文字颜色

### R4: AI对话侧滑改为调用抽屉栏
- AI对话界面的右滑手势从打开历史对话抽屉改为打开主应用抽屉栏
- 删除顶栏原有的侧边栏图标（手势触发已足够）

### R5: 标签与活动联动增强
- **活动→标签**：在添加行为弹窗中选择活动时，自动选中该活动绑定的所有标签
- **标签→活动**：在添加行为弹窗中选择标签时，自动选中该标签绑定的活动（若标签无绑定活动则不改变当前活动选择）
- **多标签冲突**：多个标签同时被选择时，活动取最后一个标签绑定的活动
- **备注自动识别**：NoteMatcher 匹配结果同样遵循上述联动规则

## Assumptions (resolved)

* tag_groups 表（v14新增）的分组信息可作为标签分类的来源
* 行为弹窗的标签选择是多选，活动选择是单选——联动规则需要适配这个差异
* **"最后一个标签"= 点击时序最后**（最近一次被用户点击选中的标签）

## Open Questions

(none)

## Acceptance Criteria (evolving)

- [ ] 新增标签弹窗默认图标为 `hi:Tag01`（hi 图标库标签图标）
- [ ] 行为弹窗中新增标签可选择分类，且能看到已有分类列表
- [ ] 抽屉栏总用时区域使用强调色背景+对比文字
- [ ] AI对话页面右滑打开主抽屉栏，历史对话仅通过图标触发
- [ ] 选择活动时自动选中其绑定标签
- [ ] 选择标签时自动选中其绑定活动
- [ ] 多标签选择时活动取最后一个标签的绑定活动
- [ ] 备注自动识别遵循相同联动规则
- [ ] AI对话页面无侧边栏图标，仅靠手势触发主抽屉
- [ ] AI调用行为工具时关键词匹配生效

## Definition of Done

* Lint / typecheck 通过
* 手动验证各场景

## Out of Scope

* 标签与活动绑定关系的编辑界面（已有）

### R6: 行为工具关键词匹配增强
- AI 调用 startBehavior / recordBehavior 等工具时，tag/activity 的 keywords 字段应参与匹配
- 当前 startBehavior 传入 tagName 时只按名称精确匹配，不匹配 keywords
- 需要hook各个行为相关工具函数，让关键词匹配默认生效
- 复用 NoteMatcher 或 MatchHelpers 中已有的关键词解析逻辑

## Technical Notes

### 关键文件
- `core/designsystem/.../form/ActivityFormSpecs.kt` — 图标默认值
- `core/behaviorui/.../sheet/SheetPickerSection.kt` — categories 传参
- `core/behaviorui/.../sheet/AddBehaviorSheetContent.kt` — 选择逻辑
- `core/behaviorui/.../sheet/AddBehaviorState.kt` — 状态管理
- `core/tools/.../match/NoteMatcher.kt` — 备注匹配
- `app/.../component/AppDrawer.kt` — 抽屉栏样式
- `app/.../experimental/ai_inter/chat/AiAssistantChatRoute.kt` — AI对话侧滑
- `core/data/.../dao/TagDao.kt` — getTagIdsForActivitySync / getActivityIdsForTagSync
- `core/data/.../dao/ActivityDao.kt` — getTagIdsForActivitySync
