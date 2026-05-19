# AI 导航重构与工具页面优化

## 需求概述

对 AI 相关页面进行导航重构和 UI 优化，提升用户体验。

## 详细需求

### 1. AI 助手对话页面提升到底部导航
- 将 AI 助手对话页面（`ai_assistant_chat`）添加到底部导航胶囊中
- 从 AI Inter 页面中删除"AI 助手对话"入口卡片
- 底部导航变为 4 个项：主页、统计、设置、AI 助手

### 2. AI 助手对话页面添加 AI Inter 入口
- 在 AI 助手对话页面的右上角添加一个按钮，点击后导航到 AI Inter 页面
- 用于配置、调试等高级功能

### 3. 输入框自动聚焦
- 进入 AI 助手对话页面时，自动聚焦到输入框
- 触发输入法弹出

### 4. 提示词配置开放
- 将现有提示词显示开放给用户配置
- 需要确认具体需求范围

### 5. 工具页面分组展示
- 在 AI 工具列表页面中，按 `ToolCategory` 分组展示工具
- 每个分组显示分类标题

## 涉及文件

- `app/.../component/AppBottomNavigation.kt` - 底部导航组件
- `app/.../navigation/NLtimerRoutes.kt` - 路由定义
- `app/.../NLtimerScaffold.kt` - 脚手架（底部栏可见性）
- `app/.../experimental/ai_inter/AiInterScreen.kt` - AI Inter 页面
- `app/.../experimental/ai_inter/chat/AiAssistantChatRoute.kt` - AI 助手对话页面
- `app/.../experimental/ai_inter/chat/components/ChatTopBar.kt` - 对话顶栏
- `app/.../experimental/ai_inter/chat/components/ChatInput.kt` - 输入组件
- `app/.../experimental/ai_inter/AiInterSubScreens.kt` - AI Inter 子页面（工具列表、提示词配置）

## 验收标准

1. 底部导航显示 AI 助手入口，点击可进入对话页面
2. AI Inter 页面不再显示"AI 助手对话"卡片
3. AI 助手对话页面右上角有 AI Inter 入口按钮
4. 进入对话页面时输入框自动聚焦、输入法弹出
5. 工具页面按分类分组展示
