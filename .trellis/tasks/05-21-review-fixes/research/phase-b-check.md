# 阶段 B 质量检查报告

> 时间：2026-08-01  
> 角色：trellis-check（唯一允许编译的子代理）  
> 命令：`.\gradlew.bat :app:compileDebugKotlin --no-daemon`

---

## 1. 变更概览

| 项 | 数值 |
|----|------|
| 变更源码文件 | 52（含 5 个删除的 form renderer） |
| 净 diff | +560 / -779 |
| 审查报告 | 6 份 `*-review.md` |
| 本阶段 check 额外修复 | 1 处编译错误 |

### 变更模块分布

- **core/data**：BehaviorRepositoryImpl stale return、DataExportImport、StatsQuery、TimeSnap、SettingsPrefs、DatabaseModule
- **core/tools**：JSON 返回合规、ActivateGoal `ACTIVE.key`、参数校验、match/timing/library 多工具
- **core/ai**：SSE 多行 JSON 解析、tool_call id 一致性
- **core/behaviorui / designsystem**：AddBehaviorSheet maxLines、FabDrag 命名、form renderer 合并删除
- **feature/home**：HomeSheetRouter PENDING startTime 崩溃修复
- **feature/ai**：流式竞态、Highlighter 泄漏、markdown 安全协议、regenerate 重复 user
- **feature/stats/settings/behavior_management**：嵌套 Scaffold、CancellationException、Paint 分配
- **app**：Scaffold 标题、AppTopAppBar 未用参数、NavHost 整理

---

## 2. 编译验证

### 首次编译（失败）

```
e: feature/ai/.../AiInterViewModel.kt:329:42 Unresolved reference 'job'.
BUILD FAILED in 2m 40s
```

**根因**：阶段 A 为修复流式竞态引入：

```kotlin
val job = viewModelScope.launch {
    // ...
    if (currentStreamJob === job) { ... }  // lambda 内前向引用尚未初始化的 val
}
currentStreamJob = job
```

Kotlin 不允许在 `launch` 的 lambda 内引用尚未完成初始化的 `val job`。

### 修复（本 check 代理）

文件：`feature/ai/src/main/java/com/nltimer/feature/ai/viewmodel/AiInterViewModel.kt`

- 改为 `currentStreamJob = viewModelScope.launch { ... }` 直接赋值
- 协程内用 `val thisJob = coroutineContext[Job]` 做身份守卫
- 保持原意图：`clearRequested` + 任务身份守卫，避免清空/新任务后旧 finally 污染状态

### 二次编译（通过）

```
.\gradlew.bat :app:compileDebugKotlin --no-daemon
BUILD SUCCESSFUL in 2m 11s
180 actionable tasks: 22 executed, 158 up-to-date
```

警告（非阻断）：
- `BehaviorManagementViewModel.kt` annotation target 未来行为提示
- `AppTopAppBar.kt` Experimental Haze Materials API

---

## 3. 关键修复抽查

| 检查项 | 结果 |
|--------|------|
| form renderer 删除残留引用 | ✅ 无 `IconColorRenderer` 等外部引用；逻辑在 `FormRowRenderers.kt` |
| `AppTopAppBar` 删除 `isImmersive`/`momentSortLabel` | ✅ 调用点已同步；主题 `isImmersive` 仍正常使用 |
| `MAX_OPTIONS_PER_ROW` 重命名 | ✅ 无 `MaxOptionsPerRow` 残留 |
| `ActivateGoalTool` 使用 `BehaviorNature.ACTIVE.key` | ✅ 与 Repository/`"active"` 约定一致 |
| `HomeSheetRouter` PENDING startTime | ✅ `cell.startEpochMs ?: 0L`，避免崩溃 |
| `BehaviorRepositoryImpl` 激活后 re-read | ✅ `getById` 返回最新 ACTIVE 实体 |
| tool_call id 一致性 | ✅ `buildAssistantToolMessage` 统一分配并写回 buffer |
| `AiAssistantChatViewModel` regenerate | ✅ `persistUserMessage = false`；DB 写包 `runCatching` |
| StatsScreen 嵌套 Scaffold | ✅ 无内层 Scaffold（符合 common-bug #1） |

---

## 4. 文档同步（可选）

```
python scripts/check-docs-sync.py
```

**结果：有差异（非本轮引入，未在阶段 B 强改）**

- 文档未记录模块：`core:ai`, `feature:ai`
- `ActivityTagBindingEntity` 源码有 `source` 字段，文档未记录

建议后续单独任务更新 `docs/agent/03-module-map.md` / `05-data-model.md`。

---

## 5. 本阶段修改/创建的文件

| 文件 | 动作 |
|------|------|
| `feature/ai/.../AiInterViewModel.kt` | **修复编译错误**（job 前向引用） |
| `.trellis/tasks/05-21-review-fixes/research/phase-b-check.md` | **新建**（本报告） |

其余 52 文件为阶段 A 并行审查落代码，本阶段未回滚。

---

## 6. 风险点摘要

| 风险 | 级别 | 说明 |
|------|------|------|
| `AiInterViewModel` 流式身份守卫 | 低 | 已用 `coroutineContext[Job]` 修复编译；逻辑与原 `=== job` 等价 |
| Highlighter 每代码块独立引擎 | 中（已记 P2） | DisposableEffect 已 destroy；全屏共享更优但未做 |
| form renderer 合并 | 低 | 无外部引用，编译通过 |
| 文档漂移 core:ai / feature:ai | 低 | 不影响编译与运行 |
| 未跑 detekt 全量复检 | 信息 | 阶段 B 以 compile 为主；detekt 下降可作为后续可选 |
| 未安装到设备 | 按约定 | 主代理负责 install；失败重连 `100.99.129.110:5555` |

---

## 7. 结论

| 项 | 状态 |
|----|------|
| 编译 `:app:compileDebugKotlin --no-daemon` | ✅ **通过** |
| 编译错误修复 | ✅ 1 处（AiInterViewModel job） |
| 关键修复意图保持 | ✅ 未回滚功能修复 |
| 删除 renderer 无残留 | ✅ |
| 文档同步 | ⚠️ 预存差异，可选后续修 |
| git commit | 未执行（按约定） |
| 安装设备 | 未执行（主代理负责） |

**阶段 B 完成。主代理可进行安装到设备。**
