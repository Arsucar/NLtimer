# 项目全方位文档：开发者/用户/Agent三类文档

## Goal

为 NLtimer 项目创建三类目标受众不同的文档体系，替换现有 `docs/` 目录内容：
1. **开发者文档** — 帮助开发者快速定位代码中的逻辑、函数、UI 组件
2. **用户文档** — 帮助用户快速上手软件，了解核心流程和高级特性
3. **Agent 文档** — 帮助 AI Agent 以最少 token 理解项目全貌

## What I already know

* 项目是 Android 原生应用（Kotlin + Jetpack Compose），行为记录与时间管理工具
* 多模块架构：app、core/{data, designsystem, behaviorui, debugui, tools}、feature/{home, stats, settings, categories, management_activities, tag_management, behavior_management, sub, debug}
* MVVM + 单 Activity + Hilt DI + Room 数据库（6 张表，版本 12）
* 现有文档位于 `docs/` 和 `docs/ai-context/`，将被替换
* 全中文编写
- 子目录结构：docs/developer/、docs/user/、docs/agent/

## Requirements

### 通用要求

- 全中文
- 删除现有 `docs/` 目录全部内容，重写为新三类体系
- 保留 `docs/reference/` 和 `docs/myNote/` 目录不动（开发者个人笔记和参考）
- Markdown 格式

### 开发者文档 (docs/developer/)

目标：帮助开发者快速定位某种逻辑、函数或 UI 组件的查找文档

- **00-索引.md** — 所有开发者文档的导航索引
- **01-架构概览.md** — 分层架构、模块职责、数据流向图
- **02-模块详解.md** — 每个模块的职责、关键类、对外 API
- **03-数据模型.md** — Room 实体、关系、迁移版本摘要
- **04-导航与路由.md** — NavHost 路由表、导航流程
- **05-组件库.md** — designsystem 可复用组件清单、参数说明
- **06-代码约定.md** — 命名规范、文件组织、设计模式

### 用户文档 (docs/user/)

目标：快速指南风格，核心流程分步说明，精简实用，不截屏

- **00-索引.md** — 用户文档导航
- **01-快速上手.md** — 核心使用流程（创建活动 → 记录行为 → 查看时间线）
- **02-活动管理.md** — 活动创建/编辑/分组/归档/关键词
- **03-标签系统.md** — 标签创建/分类/绑定
- **04-行为记录.md** — 计时、补记、计划、番茄钟、成就等级、备注匹配
- **05-首页视图.md** — 四种布局模式（网格、时间线、日志、此刻）及操作
- **06-统计.md** — 统计功能说明
- **07-设置与个性化.md** — 主题、调色板、对话框配置、首页布局配置
- **08-数据管理.md** — JSON 导入导出、行为记录管理

### Agent 文档 (docs/agent/)

目标：以最少 token 帮助 AI Agent 快速理解项目，填充上下文的文档

- **00-index.md** — Agent 文档入口，指引加载顺序
- **01-project-overview.md** — 项目一句话描述、技术栈、版本、构建信息
- **02-architecture-digest.md** — 精简架构图（分层、模块依赖、数据流）
- **03-module-map.md** — 模块 → 关键类 → 文件路径的快速映射表
- **04-patterns.md** — 项目中使用的设计模式、约定、惯用法
- **05-data-model.md** — 精简数据模型（实体、关系、字段速查）

## Acceptance Criteria

- [ ] `docs/developer/` 包含上述 7 个文件
- [ ] `docs/user/` 包含上述 9 个文件
- [ ] `docs/agent/` 包含上述 6 个文件
- [ ] 旧的 `docs/ai-context/`、`docs/database-models.md`、`docs/code-review-report.md` 等已删除
- [ ] `docs/reference/` 和 `docs/myNote/` 保持不动
- [ ] 所有文档为中文
- [ ] 内容基于实际代码探索，非模板占位

## Definition of Done

- 文件全部创建完成
- 内容准确反映当前代码状态（版本 0.1.5）
- README.md 中的文档链接更新指向新结构

## Out of Scope

- 不修改应用代码
- 不截屏/不制作图片
- 不修改 `docs/reference/` 和 `docs/myNote/`
- 不更新 CHANGELOG

## Technical Notes

- 现有文件需删除：`docs/ai-context/` 目录、`docs/database-models.md`、`docs/code-review-report.md`、`docs/refactor-optimization-scout.md`、`docs/UNIT_TEST_AUDIT.md`
- Agent 文档命名用英文文件名（方便 Agent 加载），内容用中文
- 开发者和用户文档文件名也用英文（兼容性），内容中文
