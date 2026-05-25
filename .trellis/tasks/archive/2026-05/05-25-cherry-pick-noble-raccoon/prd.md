# cherry-pick noble-raccoon overnight refactor 到 dev-v5

## Goal

将 noble-raccoon 分支的 overnight refactor 提交 (247afe4a) cherry-pick 到 dev-v5，补上 keen-walrus 未覆盖的 tools/数据层优化。

## 背景

keen-walrus 和 noble-raccoon 是同一任务（overnight Detekt/Compose 优化）的并行分支：
- keen-walrus (3301b504) → 已合入 dev-v5（侧重 home/tag 组件层）
- noble-raccoon (247afe4a) → 未合并（侧重 tools/数据层 + 导航 + 行为/活动管理）

noble 独有的关键改动：
- `BatchCreateActivityCategoriesTool`, `BatchCreateTagsTool`, `ApplyNoteDirectivesUseCase` 等 tools 层优化
- `GetDailySummaryTool` (+57), `GetWeeklySummaryTool` (+27), `RecordBehaviorTool` (+21)
- `NoteDirectiveParser`, `NoteMatcher` 工具链优化
- `NLtimerNavHost`, `DataExportImportRepositoryImpl` 导航/数据层改进
- `BehaviorManagementScreen` (+36), `ActivityManagementScreen` (+12)

## Requirements

- [ ] cherry-pick `247afe4a` 到 `dev-v5`，无冲突

## Acceptance Criteria

- [ ] cherry-pick 成功应用，无 merge conflict
- [ ] git log 确认 `247afe4a` 已在 dev-v5 上

## Technical Approach

直接 `git cherry-pick 247afe4a`，已 dry-run 验证无冲突。

## Out of Scope

- noble-raccoon 的未提交改动（暂存区 42 文件 + 工作区 30 文件）不处理
