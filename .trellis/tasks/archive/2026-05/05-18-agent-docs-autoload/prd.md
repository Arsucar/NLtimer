# Agent 文档自动加载与更新机制

## Goal

让新 AI 会话启动时自动加载 `docs/agent/` 中的精简上下文文档，并提供更新校验机制确保文档与代码同步。

## What I already know

* `docs/agent/` 已有 6 个精简文档（00-index 到 05-data-model）
* AGENTS.md 是自动注入入口（`<!-- TRELLIS:END -->` 之后的内容不受 Trellis 管理）
* 校验脚本放 `scripts/check-docs-sync.py`

## Requirements

### 1. AGENTS.md 引用

在 AGENTS.md 的 `<!-- TRELLIS:END -->` 之后添加 Agent 文档引用说明：
- 指向 `docs/agent/00-index.md` 作为入口
- 说明加载顺序和用途
- 供任何 AI 工具（不限于 Trellis）读取

### 2. 更新校验脚本

创建 `scripts/check-docs-sync.py`，用于：
- 对比 `docs/agent/` 中的模块映射与实际源码目录结构
- 检查数据模型文档中的字段是否与 Room Entity 一致
- 输出差异报告

### 3. 维护说明

在 `docs/agent/00-index.md` 末尾添加：
- 文档应在以下情况更新：模块增删、Entity 字段变更、架构调整
- 指向校验脚本

## Acceptance Criteria

- [ ] 新 AI 会话启动时能看到 Agent 文档的引用指引
- [ ] `scripts/check-docs-sync.py` 存在且可运行
- [ ] 脚本能检测出模块增删和数据模型字段差异
- [ ] `docs/agent/00-index.md` 包含维护说明

## Definition of Done

- 脚本可运行，输出有意义的结果
- AGENTS.md 更新不破坏 Trellis 管理区域

## Out of Scope

- 不自动生成/更新文档内容（仅检测+报告）
- 不修改 CI 配置
- 不修改 Trellis 管理区域

## Technical Notes

- AGENTS.md 中 Trellis 管理区域外的内容不受 `trellis update` 影响
- 校验脚本用 Python，读取源码做简单文本匹配（不依赖编译）
- 脚本位置：`scripts/check-docs-sync.py`
