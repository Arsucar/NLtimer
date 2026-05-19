<!-- TRELLIS:START -->
# Trellis Instructions

These instructions are for AI assistants working in this project.

This project is managed by Trellis. The working knowledge you need lives under `.trellis/`:

- `.trellis/workflow.md` — development phases, when to create tasks, skill routing
- `.trellis/spec/` — package- and layer-scoped coding guidelines (read before writing code in a given layer)
- `.trellis/workspace/` — per-developer journals and session traces
- `.trellis/tasks/` — active and archived tasks (PRDs, research, jsonl context)

If a Trellis command is available on your platform (e.g. `/trellis:finish-work`, `/trellis:continue`), prefer it over manual steps. Not every platform exposes every command.

If you're using Codex or another agent-capable tool, additional project-scoped helpers may live in:
- `.agents/skills/` — reusable Trellis skills
- `.codex/agents/` — optional custom subagents

Managed by Trellis. Edits outside this block are preserved; edits inside may be overwritten by a future `trellis update`.

<!-- TRELLIS:END -->

## 项目上下文文档

Agent 快速上下文文档位于 `docs/agent/`，按需加载：

1. `docs/agent/01-project-overview.md` — 项目基本信息（先读）
2. `docs/agent/02-architecture-digest.md` — 架构与数据流
3. `docs/agent/03-module-map.md` — 模块→关键类→文件路径映射（定位代码时读）
4. `docs/agent/04-patterns.md` — 设计模式与代码约定（写代码时读）
5. `docs/agent/05-data-model.md` — 数据模型速查（涉及数据层时读）

入口索引：`docs/agent/00-index.md`

文档与代码同步校验：`python scripts/check-docs-sync.py`

## 用户自主添加规则

1. 如果用户要求创建工作树，默认创建在 `.worktrees` 中，避免创建在工作区外因为工作区不一致导致需要频繁申请读写权限。
