# 全项目全流程全链路审查与深度优化

## Goal

因 API 余额次日过期，不记成本地并行处理：对 NLtimer（Android 行为记录与时间管理应用，481 个 Kotlin 文件 / 约 2.6MB 源码）进行极其细致的全流程与全链路审查，随后执行深度优化。

## What I already know

* 项目：单 Activity + Compose + Hilt + Room（6 表，版本 12），多模块（app / feature/* / core/*）
* 三层架构：Composable(Route) → ViewModel(StateFlow) → Repository → UseCase → DAO → Room
* 分支 dev-v6，基础版本 0.3.7
* 已有研究资料：`research/detekt-issues-filtered.txt`（335 个源码 issue）、`research/detekt-report.md`（按类别汇总与 P0/P1/P2 优先级建议）
* 主要技术债类别（来自 detekt）：LongMethod(36) / LongParameterList(19) / TooGenericExceptionCaught(16) / CyclomaticComplexMethod(9) / MagicNumber(72) / MaxLineLength(57) / UnusedParameter(39) / UnusedPrivateProperty(17) 等
* 常见坑记录在 `docs/agent/06-common-bug.md`；agent 文档同步校验脚本 `scripts/check-docs-sync.py`

## Assumptions (temporary)

* 审查与优化需要覆盖：数据层(Room/DAO/Repository)、业务层(UseCase/Tool)、UI 层(Compose Screen/ViewModel)、导航链路、错误处理、性能、并发安全、文档同步
* 优化目标以"正确性 + 可维护性 + 性能"为主，不做破坏性 API 变更
* 并行切分：按模块/层横向切分为多个独立审查子代理，最后汇总深度优化

## Open Questions

（无）

## Requirements (evolving)

* 按「链路+层」混合切分为 6 路并行子代理，全项目全流程全链路审查：
  1. 数据层（Room/DAO/Repository/Entity/Model）
  2. 业务层（UseCase/Tool 工具注册链）
  3. UI 层 A（feature/home + core/behaviorui 行为添加面板链路）
  4. UI 层 B（feature/stats + settings + management_activities + categories + tag_management）
  5. AI 聊天链路（experimental/ai_inter：ViewModel/网络/流式/渲染）
  6. 导航 + 壳（NLtimerScaffold/NavHost/路由/ToolRegistry 注册链）
* 深度优化范围：**bug 优先 + 低风险重构** —— 优先修复 bug/并发/性能/空安全/死代码；同时修复确定性高的 detekt P0；复杂重构（超大函数拆解等）仅记录不改
* 汇总审查发现，分级（P0/P1/P2）输出
* 对确认的缺陷与高价值优化点执行代码修改
* 修改后进行构建验证（--no-daemon）与 detekt 复检

## Acceptance Criteria (evolving)

* [x] 产出全项目审查报告（6 路 research/*-review.md + phase-b/b2-check.md）
* [x] 高价值缺陷已修复并编译通过（两轮 `:app:compileDebugKotlin` 均成功；首轮 installDebug 成功）
* [x] 第二轮深度优化：首页 P1（PENDING/linkage/ACTIVE 校验/MomentView/预估时长）、AI（clearCurrent/modelsError/空会话）、数据层（编辑冲突/成就重算/SMART 事务）
* [ ] detekt issue 数较基线（335）有下降（本轮未跑全量 detekt 复检，以 compile 为主）
* [ ] 文档与代码同步校验通过（check-docs-sync.py 有预存差异，非本轮引入）

## Definition of Done (team quality bar)

* 构建通过（gradlew --no-daemon）
* detekt 复检无新增 P0
* 文档同步校验通过
* 审查与优化记录写入任务 research/ 与 prd

## Out of Scope (explicit)

* 破坏性 API / 数据库结构变更（Room schema 改动）
* 大规模 UI 重设计
* 依赖版本升级

## Implementation Plan (并行)

* 阶段 A（并行 6 路 trellis-implement，只审查+修复，不编译）：
  1. 数据层：core/data（DAO/Entity/Repository/UseCase/DI/migration）
  2. 业务层：core/tools（Tool 注册链/timing/match/library/event）
  3. UI-A：feature/home + core/behaviorui
  4. UI-B：feature/stats + settings + management_activities + categories + tag_management + behavior_management
  5. AI 链路：feature/ai + core/ai（网络/流式/toolcall/渲染）
  6. 导航+壳：app（Scaffold/NavHost/Routes/component）+ core/designsystem + core/debugui + feature/debug/sub
* 阶段 B：最后 1 路 trellis-implement/check 汇总编译（--no-daemon），修复编译错误，detekt 复检
* 阶段 C：主代理安装到设备（失败重连 100.99.129.110:5555）

## 审查输出约定

每路子代理产出 `research/<slug>-review.md`：按模块列出 P0(bug)/P1(性能并发)/P2(整洁) 发现，每条含文件:行、问题、修复建议。低风险修复直接落代码。

## Technical Notes

* 结构参考 `docs/agent/03-module-map.md`（模块→类→路径）
* 架构参考 `docs/agent/02-architecture-digest.md`
* detekt 基线：335 个源码 issue（research/detekt-issues-filtered.txt）
* 构建注意：gradle 命令必须加 --no-daemon；多个子代理中只允许最后一个检查子代理编译，避免内存耗尽
* 安装流程：主代理最后必须执行安装到设备；失败时重连 100.99.129.110:5555
