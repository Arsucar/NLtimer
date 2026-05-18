# 架构摘要

## 三层结构

```
app (壳模块: MainActivity, NavHost, Scaffold, Drawer)
 ├── feature/* (功能模块: 各 Screen + ViewModel)
 │    home, stats, settings, categories,
 │    management_activities, tag_management,
 │    behavior_management, sub, debug
 └── core/* (基础模块: 共享逻辑与 UI)
      data          — Room DB, Repository, UseCase, Model
      designsystem  — Theme, 组件库, 表单系统, 图标系统
      behaviorui    — 行为添加面板及子组件
      debugui       — Debug 共享组件
      tools         — Tool Registry (计时/匹配/笔记处理)
```

## 数据流

```
Composable(Route) → Screen(UI)
    ↓ event
ViewModel(@HiltViewModel)
    ↓ StateFlow<UiState>
Repository (interface → impl)
    ↓ Domain Model (model/)
UseCase (usecase/)    ← 共享业务逻辑
    ↓
DAO → Room DB (entity/)
```

## 关键约定

- `*Route.kt` 注入 ViewModel，`*Screen.kt` 纯展示
- Entity ↔ Model 通过 `fromEntity()` / `toEntity()` 转换
- Repository 接口在 `repository/`，实现在 `repository/impl/`
- ViewModel 暴露 `StateFlow<UiState>`，UI 用 `collectAsStateWithLifecycle` 收集
- 单 Activity：所有页面为 Compose Navigation Destination

## 导航结构

```
MainActivity → NLtimerApp → NLtimerScaffold (Drawer + TopBar + BottomNav)
  └─ NLtimerNavHost
       底栏: home | stats | categories | management_activities | settings
       全屏子页: theme_settings, dialog_config, data_management,
                 behavior_management, home_layout_config,
                 color_palette, tag_management, ai_inter, debug/*
```
