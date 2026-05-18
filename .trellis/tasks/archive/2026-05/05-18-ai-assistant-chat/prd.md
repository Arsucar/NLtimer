# AI 助手对话页实现

## 设计文档

完整设计规格见：[docs/superpowers/specs/2026-05-18-ai-assistant-chat-design.md](../../../docs/superpowers/specs/2026-05-18-ai-assistant-chat-design.md)

## 实现计划

分阶段详细实现计划见：[docs/superpowers/plans/2026-05-18-ai-assistant-chat-plan.md](../../../docs/superpowers/plans/2026-05-18-ai-assistant-chat-plan.md)

## 概要

在 AI Inter 板块新增独立路由 `AI_ASSISTANT_CHAT`，实现面向最终用户的多会话 AI 对话界面：

- 多会话管理（创建/切换/重命名/删除）
- SSE 流式对话 + 工具调用多轮循环（复用 AiInterApiClient + ToolRegistry）
- Markdown 渲染（GFM + 代码高亮 + 表格 + 图片）
- rikkahub 风格 UI（毛玻璃输入栏 + 抽屉 + 圆角气泡 + 跳转浮动按钮）
- 对话导出（Markdown / JSON）

## 实施阶段（7 阶段 / 16 任务）

| 阶段 | 范围 | 主要交付 |
|---|---|---|
| 1 | 依赖 + 数据层 + Migration | libs + Entity/DAO + Migration_3_4 + 测试 |
| 2 | ConversationExporter | Markdown / JSON 序列化 + snapshot tests |
| 3 | ViewModel + 工具循环 | AiAssistantChatViewModel + 集成测试 |
| 4 | highlight 模块 + Markdown 核心 | Highlighter + MarkdownBlock（GFM+代码+图片） |
| 5 | Markdown 表格 | DataTable + HtmlTable 分支 |
| 6 | UI 组件全套 | ChatTopBar/Input/List/Message/Drawer/ExportSheet + 路由 |
| 7 | 毛玻璃 + 跳转按钮 | hazeEffect + MessageJumper + 端到端烟测 |

## 关键修正（相对 spec）

- DB 版本：实际 AiInterDatabase 当前 version=3，正确升级是 v3→v4 + Migration_3_4
- 测试位置：项目无 app/src/test/，所有 Room/Compose 测试走 app/src/androidTest/
