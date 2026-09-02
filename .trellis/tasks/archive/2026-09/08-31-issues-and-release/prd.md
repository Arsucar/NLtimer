# 处理全部 Issue 并发布新版本

## Goal

关闭当前唯一未关闭 GitHub Issue（#7 归档区），验证无误后 bump 版本并推送 `v*.*.*` tag，由 GitHub Actions 发布 APK。

## Domain terms (do not mix)

| 中文 | 模型 | 是什么 | 管理入口 | 当前「删除」 |
|------|------|--------|----------|--------------|
| **行为** | `Behavior` | 一条时间记录（主页列表项） | 主页 / 行为管理 | 硬删除该条记录（与本 Issue 无关） |
| **活动** | `Activity` | 行为的分类定义（如「阅读」） | 活动管理 | **删除**=硬删（含该活动下行为）；**归档**=编辑表单「归档」开关，保存后从列表消失 |
| **标签** | `Tag` | 行为的分类定义（如「专注」） | 标签管理 | **归档**=编辑表单「归档」开关；底部「删除标签」实际也走 `setArchived(true)`（文案仍写不可撤销） |

Issue #7 要做的是 **活动 + 标签** 的归档区，**不是行为**。

## What I already know

* 仓库 `Arsucar/NLtimer`，当前分支 `dev-v6`。
* 未关闭 Issue 仅 **#7**（enhancement）：归档区页面，查看/恢复已归档活动与标签。
* Issue **#6** 已 `not_planned` 关闭（同一痛点的早期描述，被 #7 取代）。无开放 PR。
* 标签「删除」=`TagRepository.setArchived(id, true)`（软删除）。**没有任何生产代码调用 `setArchived(..., false)`**。
* 活动「删除」=`ActivityManagementRepository.deleteActivity(id)`（**硬删除**：清绑定、行为、再 `deleteById`）。Issue #7 写「活动同理走归档」与代码不符。
* DAO/Repo 有 `getAllActive` / `getAll` / `setArchived`，**没有 `getArchived()`**。`ActivityManagementRepository` 未暴露归档 API。
* `name` 全表唯一（含已归档行），恢复本身不会撞 unique index；冲突只发生在「新建同名」路径。
* `setArchived` SQL **不写 `archivedAt`**。
* 分组 Entity 有 `isArchived`/`archivedAt` 列，但 Group DAO 无归档 API。
* 导航：`NLtimerRoutes.MANAGEMENT_ACTIVITIES` / `TAG_MANAGEMENT` 为顶层 composable；管理页无独立 TopAppBar（外层 `NLtimerScaffold`），设置齿轮菜单目前只有配色项。
* 最新 GitHub Release：`v0.3.10`。`gradle.properties` 仍是 `APP_VERSION_NAME=0.3.7` / `APP_VERSION_CODE=307`（与 tag 脱节）。
* `v0.3.10..HEAD` 已有 2 个 commit：多模块 bug 修复、Issue/PR 工作流。
* 发布方式：推送 `v*.*.*` tag，不在本地打包装；changelog 双语、条目 ≤20、避免技术名词。见 `.trae/skills/publish-release.md`。
* `docs/agent/01-project-overview.md` 版本仍写 0.1.5，发布时需同步。

## Assumptions (temporary)

* 只处理 **open** Issue；已关闭的 #6 不重开。
* 分组归档不在本次范围。
* 归档区不做硬删除（Issue 标可选）。
* 下一版本号为 **0.3.11 / 311**（接 v0.3.10）。
* 发布前 changelog 需用户确认（publish-release 技能要求）。

## Open Questions

（无）

## Requirements (evolving)

* 实现 Issue #7 验收条件 AC1–AC5。
* **归档区列表交互**：条目本身不可点恢复。点条目打开详情（日期+感想+恢复按钮）。
* **归档区按分组聚合**（对齐标签管理页）：活动按 `ActivityGroup`，标签按 `category`；空为「未分类」/「默认」。组内按 `archivedAt` 降序（null 垫底）。
* **归档入口 UI（反馈后修订）**：编辑表单底部去掉归档开关+感想输入框。与「删除」同一行放文字链接「归档」。点击弹出大输入框弹窗（取消 / 确认归档）。确认后立即归档并关编辑页。
* **归档感想** `archiveNote`（可空）：只在该弹窗填写；归档区详情展示。Room 15→16。
* **入口**：活动管理 / 标签管理页齿轮菜单各加「归档区」，分别进入活动归档页、标签归档页。
* **不改现有删除/归档动作**（已确认）。活动与标签编辑表单已有「归档」开关；活动另有硬删除。本次只补「归档区」查看与恢复。
* 标签底部「删除」文案不改。
* Data：`ActivityDao`/`TagDao` 新增 `getArchived()`；Repository 暴露 `getArchived()`；恢复走 `setArchived(id, false)`。
* `ActivityManagementRepository` 补齐归档相关方法（至少 `getArchived` + `setArchived`）。
* `setArchived(true)` 应写入 `archivedAt`；`setArchived(false)` 应清空 `archivedAt`。
* 归档区不做硬删除、不做 Deep link。
* 分组归档不做。
* 实现完成后：关闭 #7、bump 版本、同步 `docs/agent/01-project-overview.md`、生成双语 changelog、用户确认后推 tag。

## Acceptance Criteria (evolving)

* [ ] AC1：活动管理、标签管理页齿轮菜单各有「归档区」，分别进入对应归档列表
* [ ] AC2：归档区能列出所有已归档的活动/标签
* [ ] AC3：对任一归档条目执行「恢复」，条目从归档区消失并回到对应正常列表
* [ ] AC4：空态、加载态、错误态均正常展示
* [ ] AC5：恢复动作有明确 Snackbar 反馈，Dark Mode/主题一致
* [ ] AC5b：点击条目不恢复；详情里的「恢复」才恢复
* [ ] AC5c：按分组/分类聚合；组内按归档时间新→旧
* [ ] AC5d：可填写并展示归档感想（可空）
* [ ] AC5e：编辑表单底部删除旁「归档」链接；弹窗大输入框 + 取消/确认归档
* [ ] AC6：现有活动硬删除、活动/标签归档开关行为保持不变
* [ ] AC7：相关单测覆盖 getArchived / restore / empty
* [ ] AC8：`:app:compileDebugKotlin --no-daemon` 通过
* [ ] AC9：Issue #7 关闭；无其他 open issue
* [ ] AC10：版本 bump 到 0.3.11/311；推送 tag 后 GitHub Release 由 Actions 生成

## Definition of Done (team quality bar)

* Tests added/updated（DAO/Repo/ViewModel）
* Lint / compile green（`--no-daemon`）
* `docs/agent/01-project-overview.md` 版本同步
* Issue #7 关闭并关联 commit
* 用户确认 changelog 后推 `v0.3.11` tag

## Out of Scope (explicit)

* 分组（ActivityGroup / TagGroup）归档 UI
* 归档区硬删除 / 清空归档
* Deep link
* 已关闭 Issue #6 的重开
* 本地构建 APK 上传（交给 GitHub Actions）
* 不处理非 issue 的新功能
* 标签「删除」文案不改
* 不合并活动和标签归档为单页 Tab

## Technical Notes

* 调研：[`research/archive-data-layer.md`](research/archive-data-layer.md)
* 调研：[`research/archive-ui-nav.md`](research/archive-ui-nav.md)
* 调研：[`research/release-process.md`](research/release-process.md)
* 发布技能：`.trae/skills/publish-release.md`
* 常见坑：`docs/agent/06-common-bug.md`（嵌套 Scaffold、CancellationException）
* 规格：`.trellis/spec/backend/{directory-structure,quality-guidelines,error-handling}.md`
* 子页面不要再套 Scaffold（外层已有 `NLtimerScaffold`）
* feature 模块互不依赖；归档页可放在各自 feature 内，或抽共享 composable 到 `core:designsystem`（仅 UI 壳）
* 入口：`NLtimerScaffold` 齿轮菜单，活动页/标签页各加「归档区」

## Research References

* [`research/archive-data-layer.md`](research/archive-data-layer.md) — 无 `getArchived()`；活动删除是硬删除；unique name 全表；`archivedAt` 未被 `setArchived` 写入
* [`research/archive-ui-nav.md`](research/archive-ui-nav.md) — 顶层路由 + 外层 Scaffold；齿轮菜单目前只有配色
* [`research/release-process.md`](research/release-process.md) — tag 触发 Actions；gradle 版本落后于 tag

## Decision (ADR-lite)

**Context**: 误以为活动没有归档入口，差点改删除语义。
**Decision**: 删除与归档已经共存（编辑表单 Switch「归档」+ 底部删除）。本次 **不改这些动作**，只做归档区列表与恢复。
**Consequences**: 归档区数据来自已有 `isArchived=true` 行；恢复 = `setArchived(id, false)`。

**Decision 2**: 入口用各管理页齿轮菜单「归档区」，两个独立页面（活动归档 / 标签归档），不合并 Tab。

**Decision 3**: MVP 只做当前需求（查看+恢复+发布）。不加归档区硬删、不改标签删除文案、不做分组归档。

## Technical Approach

1. DAO：`getArchived(): Flow<List<…>>`（`WHERE isArchived = 1 ORDER BY archivedAt DESC, name`）
2. 修正 `setArchived` SQL，同步 `archivedAt`（true 写当前时间，false 置 null）
3. `ActivityRepository` / `TagRepository` 暴露 `getArchived`；`ActivityManagementRepository` 补齐 `getArchived` + `setArchived`
4. 各 feature 内新增 Archive Screen + ViewModel；复用 Chip；不套内层 Scaffold
5. 路由：`activity_archive` / `tag_archive`，加入 `SETTINGS_FULLSCREEN_ROUTES`
6. `NLtimerScaffold` 齿轮菜单：活动页/标签页增加「归档区」并 navigate
7. 单测 + `:app:compileDebugKotlin --no-daemon`
8. 关 #7、bump 0.3.11/311、同步 overview 文档、changelog 确认后推 tag
