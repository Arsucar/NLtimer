# 审查未提交变更并发布新版本

## 背景

dev-v6 分支有 56 个未暂存文件 + 3 个已暂存文件，涉及多个模块的 bug 修复、性能优化、代码清理和重构。需要审查所有变更，修复发现的问题，然后编译并安装到设备发布新版本。

## 变更分类

### Bug 修复
1. **BehaviorRepositoryImpl**: `activateNextPending` 重新读取 DB 返回最新状态；`updateBehavior` 重算 actualDuration 和 achievementLevel
2. **DataExportImportRepositoryImpl**: 导入包裹在事务中；ActivityTagBinding 添加 `source="activity"`（bug #6）
3. **AddBehaviorUseCase**: 编辑时执行 snap+冲突检查，传入 `ignoreBehaviorId`
4. **UpdateBehaviorTool**: 设置 endTime 时 ACTIVE 自动转 COMPLETED
5. **AiInterApiClient**: SSE JSON 解析改为先整体解析再按行回退
6. **AiChatToolHelper**: 自动生成 tool_call_id
7. **AiAssistantChatViewModel / AiInterViewModel**: clearRequested 防止 finally 写入过期状态；thisJob 身份检查
8. **Highlighter**: 修复线程泄漏；添加 DisposableEffect 释放
9. **HtmlBlockRenderer / HtmlInlineRenderer**: isSafeImageSource 安全检查
10. **StatsQueryUseCase / GetWeeklySummaryTool**: Locale.US 防止区域格式问题
11. **TimeSnapService**: 边界计算用 MILLIS_PER_MINUTE-1 替代硬编码 59_999
12. **GridCell**: 移除无效 isPlatinum 分支
13. **ListBehaviorsTool**: 添加 endMs <= startMs 校验
14. **GetWeeklySummaryTool**: date 解析移出 runCatching，返回明确错误
15. **StatsViewModel**: CancellationException 重抛

### 重构 / 清理
1. **NLtimerNavHost**: 提取 `slideComposable` 辅助函数消除 4x 重复过渡 lambda
2. **NLtimerScaffold**: 离开主页清除日期标签；补充路由标题
3. **AppTopAppBar**: 移除未使用的 `isImmersive` 和 `momentSortLabel` 参数
4. **core/designsystem**: 删除 5 个未使用的 form renderer 文件
5. **BatchCreateActivitiesTool/BatchCreateTagsTool**: 移除未使用的 MAX_BATCH_SIZE
6. **FabDragOptions**: MaxOptionsPerRow → MAX_OPTIONS_PER_ROW
7. **IconMissLogScreen**: 用 Box 替换 Scaffold+TopAppBar
8. **BarChartCard/TrendCard**: Paint 对象复用

### 文档
- docs/agent/06-common-bug.md: 新增 bug #5/#6/#7
- .trellis/spec/backend: error-handling.md / index.md 更新

## 审查重点

1. 删除的 form renderer 是否还有引用（编译会报错）
2. AiInterApiClient 的 `return@forEach` vs `return` 语义变化
3. GetWeeklySummaryTool 缩进变化是否影响逻辑
4. AddBehaviorUseCase 编辑路径新增冲突检查的测试覆盖
5. 编译通过性
6. 已暂存文件（AddBehaviorSheetContent/State, GridCell）与未暂存文件之间的一致性

## 验收标准

- [ ] 所有代码变更已审查
- [ ] 发现的问题已修复
- [ ] 编译通过（--no-daemon）
- [ ] 安装到设备成功
