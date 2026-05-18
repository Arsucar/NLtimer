# 设计模式与约定

## 架构模式

| 模式 | 说明 |
|------|------|
| MVVM | View → ViewModel(StateFlow) → Repository → DAO |
| 单 Activity | MainActivity 为唯一 Activity，Compose Navigation 驱动 |
| Repository | 接口在 `repository/`，实现在 `repository/impl/`，Hilt `@Binds` |
| UseCase | 共享业务逻辑抽取为独立类，位于 `usecase/` |
| Tool Registry | `ToolDefinition` 接口统一工具注册，支持 UI 和 AI Agent 调用 |

## 命名约定

| 规则 | 示例 |
|------|------|
| 模块名 | `feature:home`, `core:data`, `core:designsystem` |
| 包名 | `com.nltimer.feature.<name>`, `com.nltimer.core.<name>` |
| Route/Screen | `*Route.kt`(注入 ViewModel) + `*Screen.kt`(纯 Composable) |
| ViewModel | `@HiltViewModel class HomeViewModel` |
| UiState | `data class HomeUiState` |
| Entity | `*Entity.kt` + `@Entity` 注解 |
| Domain Model | 无后缀，`fromEntity()`/`toEntity()` 转换 |
| DAO | `*Dao.kt` + `@Dao` 注解 |
| Repository | `*Repository`(接口) + `*RepositoryImpl`(实现) |
| UseCase | `*UseCase.kt` |
| 导航路由 | `NLtimerRoutes` 中的常量，字符串格式 `"feature_name"` |
| 迁移 | `Migration3To4` … `Migration11To12` |

## 文件组织

```
feature/<name>/
    model/        ← UiState 等UI状态模型
    ui/           ← *Route.kt + *Screen.kt
    viewmodel/    ← ViewModel
    di/           ← Hilt Module (可选)
    match/        ← 匹配策略 (可选)
    export/       ← 导出逻辑 (可选)

core/<name>/
    database/     ← Entity + DAO + Migration
    model/        ← 领域模型
    repository/   ← 接口 + impl/
    usecase/      ← UseCase
    util/         ← 工具类
    di/           ← Hilt Module
```

## Compose 约定

- 状态提升：Screen 通过参数接收 state 和 lambda
- 副作用：`collectAsStateWithLifecycle` 收集 Flow
- 不可变集合：使用 `kotlinx-collections-immutable`
- 主题：`NLtimerTheme` 统一提供颜色/排版/形状
- 预览：`@Preview` + `@PreviewLightDark` 注解

## DI 约定

- `@HiltViewModel` 标注 ViewModel
- `@Binds` 绑定接口到实现
- `@Provides` 提供实例
- Module 放在 `di/` 包下

## 数据库约定

- 版本号递增，每次 schema 变更写 Migration
- Entity 字段名 = 数据库列名（下划线命名）
- 外键关系在 `@Entity(foreignKeys)` 中声明
- 软删除：`isArchived` + `archivedAt` 字段
