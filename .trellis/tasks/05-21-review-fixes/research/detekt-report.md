# Detekt 静态分析报告

> 生成时间：2026-05-21（第二次运行，配置已调整）  
> 分支：dev-v5  
> 配置：`config/detekt/detekt.yml`

## 概览

| 统计 | 首次 | 第二次 | 变化 |
|------|------|--------|------|
| 总 issue 数（含 docs/reference） | 7848 | 1682 | **-78.6%** |
| 项目源码 issue（排除 docs/reference） | ~2028 | **335** | **-83.5%** |

配置调整后大幅减少了 LongMethod / CyclomaticComplexMethod / ReturnCount / NestedBlockDepth / TooManyFunctions 等规则的数量，同时 MagicNumber / WildcardImport / NewLineAtEndOfFile / PackageNaming 等噪音规则已被配置抑制。

## 项目源码按规则分类（排除 docs/reference）

| 规则 | 数量 | 严重度 | 说明 |
|------|------|--------|------|
| **LongMethod** | 36 | 高 | 函数体超过 60 行，需拆分 |
| **LongParameterList** | 19 | 高 | 参数超过 6 个，需封装为 data class |
| **TooGenericExceptionCaught** | 16 | 中 | catch Exception / Throwable 过于宽泛 |
| **CyclomaticComplexMethod** | 9 | 高 | 圈复杂度 > 15，逻辑过于复杂 |
| **TooManyFunctions** | 4 | 中 | 文件/类中函数数量过多 |
| **SwallowedException** | 7 | 中 | 异常被吞掉 |
| **NestedBlockDepth** | 1 | 高 | 嵌套层级过深 |
| **ComplexCondition** | 2 | 中 | 条件表达式过于复杂 |
| **LoopWithTooManyJumpStatements** | 3 | 中 | 循环中 break/continue 过多 |
| **ReturnCount** | 1 | 中 | return 语句过多 |
| **UseCheckOrError** | 2 | 低 | 应使用 check()/error() |
| **MagicNumber** | 72 | 低 | 魔法数字（Compose UI 中常见） |
| **MaxLineLength** | 57 | 低 | 行超 120 字符 |
| **UnusedParameter** | 39 | 低 | 未使用的函数参数 |
| **UnusedPrivateProperty** | 17 | 低 | 未使用的私有属性 |
| **UnusedPrivateMember** | 4 | 低 | 未使用的私有函数 |
| **VariableNaming** | 14 | 低 | 变量命名不规范 |
| **TopLevelPropertyNaming** | 7 | 低 | 顶层属性命名不规范 |
| **ImplicitDefaultLocale** | 6 | 低 | String.format 缺少 Locale |
| **MatchingDeclarationName** | 8 | 低 | 文件名与声明不匹配 |
| **TooGenericExceptionThrown** | 4 | 中 | throw Exception() 过于宽泛 |
| **MayBeConst** | 2 | 低 | 可声明为 const |
| 其他（< 5 条的零散规则） | 12 | 低 | SpreadOperator / InvalidPackageDeclaration 等 |

## 推荐修复优先级

### P0 — 应该修复（代码质量）

1. **LongMethod (36)** — 拆分大函数
2. **LongParameterList (19)** — 封装参数为 data class
3. **CyclomaticComplexMethod (9)** — 简化复杂逻辑

### P1 — 建议修复（代码健壮性）

4. **TooGenericExceptionCaught (16)** — 精确捕获异常
5. **SwallowedException (7)** — 记录日志
6. **TooGenericExceptionThrown (4)** — 使用具体异常类型

### P2 — 可选修复（代码规范）

7. **UnusedParameter (39)** — 删除或加 `_` 前缀
8. **UnusedPrivateProperty (17)** — 清理死代码
9. **VariableNaming (14)** — 修正命名
10. **MatchingDeclarationName (8)** — 对齐文件名

## 详细 issue 列表

完整的 issue 列表见：`detekt-issues-filtered.txt`（335 条，仅项目源码）
