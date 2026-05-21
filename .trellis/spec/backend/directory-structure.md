# Directory Structure

> How backend code is organized in this project.

---

## Overview

<!--
Document your project's backend directory structure here.

Questions to answer:
- How are modules/packages organized?
- Where does business logic live?
- Where are API endpoints defined?
- How are utilities and helpers organized?
-->

Documented from `extract-ai-inter-module` task (2026-05-21).

---

## Directory Layout

```
app/                           # 壳模块: MainActivity, NavHost, Scaffold
├── src/main/java/com/nltimer/app/
│   ├── navigation/            # NLtimerRoutes, NLtimerNavHost
│   ├── component/             # AppDrawer, AppBottomNavigation
│   └── NLtimerScaffold.kt

feature/<name>/                # 功能模块: Screen + ViewModel + DI
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    └── java/com/nltimer/feature/<name>/
        ├── navigation/        # 路由常量 + NavGraph (如有导航)
        ├── di/                # Hilt Module
        ├── data/              # Repository, Database, DAO, Entity
        ├── viewmodel/         # ViewModel
        ├── chat/              # 子功能 (可选)
        │   ├── components/
        │   ├── data/
        │   └── export/
        └── <Feature>Screen.kt # UI 组件

core/<name>/                    # 基础模块: 共享逻辑与 UI
├── src/main/java/com/nltimer/core/<name>/
│   ├── network/               # 网络层 (API Client, Models)
│   ├── config/                # 配置 (Config, Provider)
│   ├── database/              # Room DB (Entity, DAO, Migration)
│   ├── model/                 # 领域模型
│   ├── repository/            # 接口 + impl/
│   └── di/                    # Hilt Module
```

---

## Module Organization

### Three-Layer Architecture

| Layer | Modules | Purpose |
|-------|---------|---------|
| **app** | `app` | Shell: MainActivity, NavHost, Scaffold, Drawer |
| **feature** | `feature:home`, `feature:ai`, `feature:stats`, ... | Domain features with UI + ViewModel |
| **core** | `core:data`, `core:ai`, `core:tools`, `core:designsystem` | Shared logic, network, config |

### Dependency Rules

```
app → feature:* → core:*
```

- **app** can depend on any feature and any core module
- **feature** can depend on core modules only (NEVER on app or other features)
- **core** can depend on other core modules only (NEVER on app or feature)

### Feature Module Extraction Pattern

When extracting code from `app/experimental/` into a `feature:` module:

1. **Create module skeleton** — `build.gradle.kts`, `AndroidManifest.xml`, `settings.gradle.kts` include
2. **Move source files** — Change package from `com.nltimer.app.experimental.<name>` to `com.nltimer.feature.<name>`
3. **Extract navigation** — Create `feature/<name>/navigation/AiRoutes.kt` (route constants) + `AiNavGraph.kt` (NavGraphBuilder extension)
4. **Clean up app references** — Replace `NLtimerRoutes.AI_*` with `AiRoutes.AI_*` in NLtimerRoutes, NLtimerNavHost, Scaffold, Drawer, BottomNav
5. **Remove duplicate network code** — If feature has `network/` files that duplicate `core:ai`, delete them; ViewModels import from `core:ai.network` directly
6. **Update DI** — Create feature-scoped Hilt Module; remove old DI module from app
7. **Verify** — `./gradlew :feature:<name>:compileDebugKotlin --no-daemon` + `./gradlew :app:compileDebugKotlin --no-daemon`

---

## Naming Conventions

| Rule | Example |
|------|---------|
| Module name | `feature:home`, `core:data`, `core:designsystem` |
| Package | `com.nltimer.feature.<name>`, `com.nltimer.core.<name>` |
| Route/Screen | `*Route.kt` (inject ViewModel) + `*Screen.kt` (pure Composable) |
| ViewModel | `@HiltViewModel class HomeViewModel` |
| NavGraph | `<Name>NavGraph.kt` with `NavGraphBuilder.<name>NavGraph()` extension |
| Routes | `<Name>Routes.kt` with `object <Name>Routes { const val ... }` |

---

## Examples

- **feature:ai** — Complete feature module with navigation, DI, data, chat, viewmodel
- **feature:home** — Typical feature module with UI + ViewModel
- **core:ai** — Core module with network, config, toolcall
