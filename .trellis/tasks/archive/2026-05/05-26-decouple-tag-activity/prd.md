# 解耦标签活动双向绑定并支持#解析自动关联

## Goal

将标签和活动的双向绑定关系改为单向绑定（标签→活动方向保留，活动→标签方向移除），同时在添加行为弹窗中支持通过#解析添加新标签时自动单向关联到已选中的活动。

## What I already know

- 当前标签和活动是双向绑定的
- 用户希望改为单向绑定，解除双向同步
- 具体场景：
  - 场景1：标签【活着】【生死疲劳】关联活动【小说】
    - 选中【活着】或【生死疲劳】时，自动选中活动【小说】
    - 但选中活动【小说】时，不自动选中【活着】和【生死疲劳】
  - 场景2：活动【计划】关联标签【规划】【专注】
    - 选中活动【计划】时，自动选中标签【规划】和【专注】
    - 但选中【规划】或【专注】时，不自动选中活动【计划】
- 在添加行为弹窗中，通过#解析添加新标签时（如 #夏洛特），如果已选中活动（如【番剧】），应自动将新标签单向关联到该活动
- 大部分场景都是单向关联

## Assumptions (temporary)

- 当前标签和活动的关联关系存储在数据库中
- 存在添加行为弹窗的UI组件
- 已有#解析添加标签的功能

## Open Questions

（已通过代码分析解决）

## Decision (ADR-lite)

**Context**: 用户希望解耦标签和活动的双向绑定关系，解除双向同步
**Decision**: 
1. 保留标签→活动方向的自动关联（选中标签时自动关联活动）
2. 保留活动→标签方向的自动关联（选中活动时自动关联标签）
3. 解除双向同步：标签关联活动后，活动不会自动关联标签；活动关联标签后，标签不会自动关联活动
4. 在通过#解析添加新标签时，如果已选中活动，自动将新标签单向关联到该活动
**Consequences**: 需要修改添加行为弹窗的逻辑，并可能需要修改标签创建的用例

## Requirements (evolving)

1. 解耦标签和活动的双向绑定关系，解除双向同步
2. 保留标签→活动方向的自动关联（选中标签时自动关联活动）
3. 保留活动→标签方向的自动关联（选中活动时自动关联标签）
4. 标签关联活动后，活动不会自动关联标签
5. 活动关联标签后，标签不会自动关联活动
6. 在添加行为弹窗中，通过#解析添加新标签时，如果已选中活动，自动将新标签单向关联到该活动
7. 标签和活动可以分别独立更改关联的对象

## Acceptance Criteria (evolving)

- [ ] 选中标签时自动关联其绑定的活动
- [ ] 选中活动时自动关联其绑定的标签
- [ ] 标签关联活动后，活动不会自动关联标签
- [ ] 活动关联标签后，标签不会自动关联活动
- [ ] 通过#解析添加新标签时，如果已选中活动，新标签自动单向关联到该活动
- [ ] 标签和活动可以分别独立更改关联的对象
- [ ] 现有的标签和活动管理功能不受影响

## Definition of Done (team quality bar)

- 测试添加/更新（单元/集成）
- Lint / typecheck / CI 通过
- 如果行为改变，文档/笔记已更新
- 现有功能回归测试通过

## Out of Scope (explicit)

- 批量修改现有标签和活动的关联关系
- 标签和活动的其他功能修改
- 修改标签管理或活动管理界面的关联逻辑
- 修改数据库表结构或关联关系的存储方式

## Edge Cases

1. 一个标签的关联活动字段中已经限制了绑定一个活动
2. 取消了自动关联的标签或活动也不回退，避免抖动

## Technical Notes

### 数据库模型
- 标签和活动的关联关系存储在 `activity_tag_binding` 表中
- 有 `ActivityTagBindingEntity` 实体
- 有两个仓库类处理关联关系：
  - `ActivityManagementRepositoryImpl.setActivityTagBindings(activityId: Long, tagIds: List<Long>)`
  - `TagRepositoryImpl.setActivityTagBindings(tagId: Long, activityIds: List<Long>)`

### 当前实现
1. 在 `AddBehaviorSheetContent.kt` 的 `applyDirectiveAndScanResults` 函数中：
   - 当通过 `@` 匹配到活动时，会自动关联该活动绑定的标签（第581-586行）
   - 当通过 `#` 匹配到标签时，如果当前没有选中活动，会自动关联标签绑定的活动（第587-592行）

2. 在 `ApplyNoteDirectivesUseCase.kt` 中：
   - 通过 `#` 解析添加新标签时，调用 `addTagUseCase(d.name, null, null, 0, null, null, null)`
   - 没有传入活动ID，所以新标签不会自动关联到任何活动

3. 在 `AddTagUseCase.kt` 中：
   - 创建标签时，如果传入了 `activityId`，会调用 `tagRepository.setActivityTagBindings(tagId, listOf(activityId))` 来关联活动

### 需要修改的文件
1. `core/behaviorui/src/main/java/com/nltimer/core/behaviorui/sheet/AddBehaviorSheetContent.kt`
   - 修改 `applyDirectiveAndScanResults` 函数，解除双向同步逻辑
   - 确保选中标签时自动关联活动，选中活动时自动关联标签，但不会触发双向同步
   
2. `core/tools/src/main/java/com/nltimer/core/tools/match/ApplyNoteDirectivesUseCase.kt`
   - 修改 `invoke` 函数，在通过#解析添加新标签时，如果已选中活动，自动将新标签单向关联到该活动
   
3. 可能需要修改 `AddTagUseCase.kt` 或创建新的用例来处理标签和活动的关联

## Research References

（暂无外部研究，基于代码分析）

## Implementation Plan

### PR1: 解耦双向绑定
- 修改 `AddBehaviorSheetContent.kt` 中的 `applyDirectiveAndScanResults` 函数
- 解除双向同步逻辑，确保选中标签时自动关联活动，选中活动时自动关联标签，但不会触发双向同步

### PR2: 支持#解析自动关联
- 修改 `ApplyNoteDirectivesUseCase.kt` 或创建新的用例
- 在通过#解析添加新标签时，如果已选中活动，自动将新标签单向关联到该活动
- 可能需要修改 `AddTagUseCase.kt` 以支持传入活动ID

### PR3: 测试和验证
- 添加单元测试
- 验证现有功能不受影响
- 验证新功能正常工作
