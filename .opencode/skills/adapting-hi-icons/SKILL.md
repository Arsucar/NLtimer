---
name: adapting-hi-icons
description: >
  Use when NLtimer 图标需求日志中有 hi 库未覆盖的关键词需要适配，
  或用户要求将 mi/emoji 图标迁移到 hi (HugeIcons) 库。
  触发词：图标适配、hi图标、HugeIcons、icon search fallback、图标关键词、icon log。
---

# 适配 HugeIcons (hi) 图标库

## 概述

NLtimer 图标系统有三层：`hi` (HugeIcons) → `mi` (Material Icons) → `emoji`。
`IconSearchEngine` 按库优先级搜索，`hi` 匹配优先。
当用户反馈日志显示某些关键词命中了 mi/emoji 而非 hi 时，需为 hi 库补充图标或关键词。

## 涉及文件（固定 3 个）

| 文件 | 路径 | 职责 |
|------|------|------|
| HugeIconCatalog | `core/designsystem/.../icon/HugeIconCatalog.kt` | hi 图标注册 + UI 渲染目录 |
| IconSearchEngine | `core/tools/.../timing/IconSearchEngine.kt` | 搜索引擎数据 + 同义词映射 |
| (可选) IconPickerSheet | `core/designsystem/.../icon/IconPickerSheet.kt` | 图标选择器 UI，一般不改 |

## 工作流

```
解析需求日志 → 查询 hi 库覆盖 → 找缺失图标 → 查库可用图标 → 编辑3处 → 编译验证
```

### Step 1: 解析需求

从用户提供的日志中提取 `(关键词, 当前命中的库)` 对。

日志格式示例：
```
2026-05-20 10:57:18 | mi | urgent
2026-05-20 10:57:11 | mi | cooking
```

含义：`mi` = 当前命中了 Material Icons 而非 hi，需补充 hi 覆盖。

**过滤已有 hi 覆盖的关键词**（避免重复工作）。

### Step 2: 查询 hi 库是否已有覆盖

在 `HugeIconCatalog.kt` 的 `icons` 列表中搜索关键词是否已在某个 entry 的 `keywords` 中。

### Step 3: 查找可用的 HugeIcons 图标

HugeIcons 库源码在 GitHub：`rikkahub/hugeicons-compose`
stroke 图标目录：`library/src/main/java/me/rerere/hugeicons/stroke/`

用 `github_get_file_contents` 获取目录列表（返回 JSON 数组），然后用正则匹配图标名：

```powershell
# 示例：搜索 cook/food 相关图标
$json = Get-Content <saved_output> -Raw
$matches = [regex]::Matches($json, '"name":"([^"]*(?:Cook|Chef|Kitchen|Food|Pot|Bowl)[^"]*)\.kt"')
$matches | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
```

### Step 4: 三处编辑

#### 4a. HugeIconCatalog.kt

**import 区**：添加新图标的 import，按字母序插入。

```kotlin
import me.rerere.hugeicons.stroke.Alert01
import me.rerere.hugeicons.stroke.Chef
```

**icons 列表**：添加新 entry 或更新现有 entry 的 keywords。

新增图标 entry 模板：
```kotlin
HugeIconEntry("Alert01", HugeIconCategory.GENERAL, { HugeIcons.Alert01 },
    listOf("alert", "warning", "urgent", "告警", "警告", "紧急")),
```

更新现有 entry：在 `listOf(...)` 中追加缺失的中英文关键词。

**关键词命名约定**：
- 英文关键词用小写：`urgent`, `cooking`, `review`
- 中文关键词用原词：`紧急`, `做饭`, `评审`
- 语义覆盖优先于字面匹配（如 `cooking` 同时加 `做饭` `烹饪` `厨房`）

#### 4b. IconSearchEngine.kt

**HUGE_ICON_DATA**：与 HugeIconCatalog 的 icons 列表保持一一对应。
每处 HugeIconCatalog 的改动，HUGE_ICON_DATA 必须同步。

```kotlin
"Alert01" to listOf("alert", "warning", "urgent", "告警", "警告", "紧急"),
```

**SYNONYM_MAP**（可选）：仅当关键词在所有图标 keywords 中都无直接匹配时才添加。

```kotlin
"cooking" to listOf("food", "restaurant", "kitchen"),
```

优先级：keywords 直接匹配 > synonym 回退。能通过 keywords 解决的不加 synonym。

### Step 5: 编译验证

```bash
./gradlew :core:designsystem:compileDebugKotlin --no-daemon
./gradlew :core:tools:compileDebugKotlin --no-daemon
```

两个模块都 BUILD SUCCESSFUL 即完成。

## 关键词 → hi 图标速查

已覆盖的常用映射（避免重复添加）：

| 关键词域 | hi 图标 |
|----------|---------|
| 代码/code | Code |
| 时间/time/clock | Clock01, Timer01, StopWatch, Hourglass |
| 日历/calendar | Calendar01, CalendarAdd01 |
| 设置/settings | Settings01 |
| 添加/add | Add01, AddCircle |
| 搜索/search | Search01, Searching |
| 通知/notification | Notification01, Notification03 |
| 文件/file | File01, Folder01 |
| 紧急/urgent/alert | Alert01, AlertCircle, AlarmClock |
| 做饭/cooking | Chef, ChefHat |
| 清单/checklist | CheckList |
| 评审/review | CheckmarkBadge01 |
| 用户/user | User, UserCircle, UserGroup |

## 常见错误

| 错误 | 原因 | 修复 |
|------|------|------|
| 编译报 `Unresolved reference` | HugeIcons 库中不存在该图标名 | 回 Step 3 确认库名 |
| 搜索不到新增图标 | HUGE_ICON_DATA 忘记同步 | 两处必须一一对应 |
| 搜索结果仍是 mi 优先 | keywords 中少了英文小写形式 | 确保关键词包含英文和中文 |
| import 顺序问题 | 未按字母序插入 import | 调整 import 顺序 |
