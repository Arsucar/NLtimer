# 处理全部 GitHub issue 并上传 debug 包到 gofile

## Goal

实现 GitHub issue **#8、#9、#14**（#15 事件记录器已从代码完全回退，待重新评估需求后再实现）。本地 `assembleDebug --no-daemon` 产出 debug APK，上传到 gofile.io。

不走正式 GitHub Release / tag（那是 `publish-release.md` 流程）。

## What I already know

* 仓库：`Arsucar/NLtimer`，分支 `dev-v6`，HEAD `d8113c1f`
* OPEN issues（仅这 4 个）：
  * [#8](https://github.com/Arsucar/NLtimer/issues/8) bug：统计编辑模式拖动排序错乱/跳回（2026-09-02 声称已修，09-04 回退，源码不在 git）
  * [#9](https://github.com/Arsucar/NLtimer/issues/9) bug：统计编辑底栏被自定义导航遮挡；缺「取消编辑」（同样回退）
  * [#14](https://github.com/Arsucar/NLtimer/issues/14) feat：时间微调「重置」→「上尾」，跳到上一条 endTime
  * [#15](https://github.com/Arsucar/NLtimer/issues/15) feat：事件记录器（`behavior_event` 表，DB 16→17）
* CLOSED 的 #2/#3/#6/#7/#10/#11/#12/#13 不处理
* 研究已落盘：`research/stats-edit-bugs.md`、`prev-end-button.md`、`behavior-event-recorder.md`、`debug-apk-gofile.md`
* 当前 Room `version = 16`；debug APK 名 `NLtimer-v0.3.11-debug.apk`；`sdk.dir` 已配置

## Assumptions (validated from issues + research)

* 「处理所有 issue」原先含 #15；**2026-09-05 用户要求完全回退事件记录器**，#15 已 reopen，实现从源码删除（DB 仍为 16）
* #15 暂不实现，等用户确认新方案
* #14 不新增 DAO；复用 `HomeUiState.lastBehaviorEndTime`；无上一条时回退「此刻」
* #14 文案进 `core/behaviorui` `strings.xml`；仓库无 `values-en`，不单独建英文本地化目录
* #8/#9 按 issue 评论中的修复思路重做（git 无法恢复 09-02 补丁）
* 仪器测试（MigrationTestHelper / DAO androidTest）因 `core/data` 无 androidTest 源集：优先 JVM 单测（Repository/UseCase Fake DAO）；迁移 SQL 写对 + schema 17 由 KSP 生成。不为此任务给 `core/data` 加 androidTest 依赖（避免扩大构建面）
* 构建：`.\gradlew.bat :app:assembleDebug --no-daemon`；仅最后一个 check 子代理允许编译

## Open Questions

* #15 事件记录器：用户要求重新评估需求。待确认入口、数据模型、与现有 `note` 关系、是否中断计时等。

## Requirements

### #8 统计拖拽排序

* `movePanel` / `removePanel` / `addPanel`：先乐观更新 `_uiState`，再 persist DataStore
* 拖拽过程中 **不** 每次相邻交换都写 DataStore；**松手后按 panel id** 一次性 persist
* 落点用 `visibleItemsInfo` + `computeGridDropIndex`（考虑 `colSpan` 网格占用），不是固定 `itemHeight=180f` 的相邻 index
* 手势中途 **不** 把 `dragOffsetY` 归零；`translationY` 跟手，避免吸附跳变
* 连续快速拖动不丢顺序、不跳回

### #9 统计编辑底栏

* 编辑底栏按 `BottomBarMode` + `LocalNavBarWidth` 给 App 叠加导航让位（参考 `CategoriesScreen`）
* CENTER_FAB / STANDARD 下「添加面板」「恢复默认」完整可点、不被遮挡
* 增加「取消编辑」按钮，调用现有 `toggleEditMode`（与右上角对勾同一退出路径；无未保存草稿可丢，不需要 snapshot rollback）
* `StatsGridContainer` 底部 contentPadding 与实际底栏高度匹配，不再只靠 160/100 dp 硬编码硬扛遮挡（可保留最小值作兜底）

### #14 「上尾」按钮

* `TimeAdjustmentComponent`「重置」改名为「上尾」
* 点击：目标 = `lastBehaviorEndTime?.withSecond(0)?.withNano(0) ?: now`（分钟精度）
* 「现在」语义不变
* 参数从 `HomeSheetRouter`（COMPLETED + CURRENT）经 Sheet → Content → Overlay → Card → Component 透传
* `onUserAdjusted` / `markUserAdjustedTime()` 路径不变
* `BehaviorManagementScreen` 编辑入口：无 `HomeUiState` 时传 `null`（回退此刻）
* 保存仍走 `TimeSnapService`，按钮不绕过冲突校验
* 文案 `stringResource` + contentDescription

### #15 事件记录器 — **已回退，待重新评估**

2026-09-05：用户要求完全回退相关代码。源码已无 `BehaviorEvent*` / `behavior_event` / Migration 16→17；`NLtimerDatabase.version = 16`。Issue #15 已 reopen。新方案未定。

### 收尾

* `compileDebugKotlin` / `assembleDebug` 均 `--no-daemon`
* 安装到已连接设备；失败则 `adb connect 100.99.129.110:5555` 再装
* 将 debug APK 上传 gofile（`GET /servers` → `POST https://{server}.gofile.io/uploadfile`，失败回退 `https://upload.gofile.io/uploadfile`）
* 关闭 #8/#9/#14/#15，issue 评论写清改动要点 + gofile 链接

## Acceptance Criteria

* [ ] #8：连续快速拖动面板排序准确持久化，无跳回；混排 colSpan 落点与网格一致；手势中无 offset 瞬归零跳变
* [ ] #9：编辑底栏在 CENTER_FAB 下不被自定义导航遮挡；有「取消编辑」
* [ ] #14：「上尾」跳到上一条 endTime；无上一条回退此刻；与「现在」语义分离；滚轮同步；`markUserAdjustedTime` 生效
* [x] #15：已从代码完全回退，issue 已 reopen，不纳入本轮交付
* [ ] `:app:assembleDebug --no-daemon` 成功（#8/#9/#14 版本）
* [ ] gofile 返回 `status=ok` 与 `downloadPage` URL
* [ ] #8/#9/#14 保持 closed；#15 保持 open

## Definition of Done

* 实现 #8/#9/#14 的 AC
* #15 代码零残留（除 task research 文档）
* JVM 单测覆盖：#8 `movePanel` 乐观更新不被旧 config 覆盖
* Lint / compileDebugKotlin `--no-daemon` 通过
* Debug APK 已上传 gofile

## Out of Scope

* CLOSED issues
* GitHub Release / 推 `v*.*.*` tag / 本地 release 包
* #15 实现（待新方案确认后再开任务）
* #15 导出、统计面板、Analytics
* #14 DAO `getLastCompletedBehavior`、`values-en` 目录
* #9 编辑模式 snapshot rollback（取消 = 退出编辑，DataStore 已持久化的排序不回滚）
* 给 `core/data` 新增 androidTest / `room-testing` 源集
* 改 `note` 字段语义或迁移历史 note 到事件表

## Technical Approach

1. **#8/#9（stats）**：ViewModel 乐观列表 + 松手 persist；Grid 用可见 item 几何算 drop index；编辑底栏 padding 抄 `CategoriesScreen` 的 `BottomBarMode`/`LocalNavBarWidth`。
2. **#14（behaviorui）**：给 `TimeAdjustmentComponent` 增加 `prevEndTime: LocalDateTime?`，沿 Sheet 调用链透传。
3. **#15（data+home）**：照抄 `conversation_message` 的 FK+order+Flow，但走主库 Repository/UseCase 分层（不要 ViewModel 直调 DAO）。迁移 SQL 参考 AI 库 `Migrations.kt` 的 CASCADE 建表。
4. **发布**：主会话最后 `assembleDebug --no-daemon` → 安装设备 → gofile → `gh issue close`。

## Decision (ADR-lite)

**Context**: 4 个 OPEN issue 规格已写在 GitHub；#8/#9 的 09-02 补丁不在 git，只能按评论重建。  
**Decision**: 一次任务做完全部 OPEN issue + debug gofile；仪器迁移测试降级为 SQL+schema 导出+JVM 单测。  
**Consequences**: #15 是本任务最大块；stats 与 home/data 文件无重叠，可并行实现后由主会话编译安装。

## Implementation Plan

* P1: #8 + #9（`feature/stats` + 少量 scaffold 已有 CompositionLocal）
* P2: #14（`core/behaviorui` + `HomeSheetRouter`）
* P3: #15（`core/data` + `feature/home` ActiveCard/详情 + agent docs）
* P4: 主会话 compile/assemble/install/gofile/close issues

## Research References

* [`research/stats-edit-bugs.md`](research/stats-edit-bugs.md) — #8/#9 现状仍是 2026-05 实现，09-02 补丁不可恢复
* [`research/prev-end-button.md`](research/prev-end-button.md) — 「重置」=「现在」；透传链 9 层
* [`research/behavior-event-recorder.md`](research/behavior-event-recorder.md) — DB v16；conversation_message 类比；建议新建文件清单
* [`research/debug-apk-gofile.md`](research/debug-apk-gofile.md) — APK 路径与 gofile 两步上传

## Technical Notes

* Gradle 必须 `--no-daemon`
* 多个子代理只有最后一个 check 允许编译
* 主代理最后必须安装到设备；失败重连 `100.99.129.110:5555`
* 嵌套 Scaffold / `BehaviorNature.key` / Flow CancellationException 等见 `docs/agent/06-common-bug.md`
* `LocalNavBarWidth` 仅 CENTER_FAB 的 `AppCenterFabBottomBar` 会写入；STANDARD 走 Scaffold 80.dp
