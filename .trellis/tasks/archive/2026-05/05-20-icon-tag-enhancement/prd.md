# 图标匹配优化与标签-活动关联

## 需求概述

### 1. 图标匹配优化

**问题**：
- 当前 `IconSearchEngine.search()` 只搜索静态图标库，不检查图标是否已被标签/活动引用
- `"小睡"` 搜索无结果 → 返回默认图标 `#`，但系统却说"已自动匹配图标"（误导）
- 单关键词搜索，无法推断相关关键词

**解决方案**：
- 支持多关键词搜索，按优先级顺序尝试
- 优先使用 hi (HugeIcons) 图标库
- 如果 hi 无结果，再搜索 mi 和 emoji
- 使用 mi/emoji 时记录到现有 `icon_search_miss` 表
- 无匹配时返回 `⁉` + 明确说明"未匹配到图标"

**搜索策略**：
```
关键词顺序：["小睡", "睡觉", "nap", "sleep"]
├── "小睡" → hi 库搜索
│   ├── 有结果 → 返回 hi 匹配
│   └── 无结果 → mi/emoji 库搜索
│       ├── 有结果 → 记录 fallback 日志，返回匹配
│       └── 无结果 → 尝试下一个关键词
├── "睡觉" → 同上
├── "nap" → 同上
└── "sleep" → 同上
```

**返回格式**：
- 匹配到 hi：`{"iconKey": "hi:Moon", "iconMatched": true}`
- 匹配到 mi/emoji：`{"iconKey": "😴", "iconMatched": true, "fallbackLibrary": "emoji", "fallbackNote": "hi库无匹配"}`
- 全部无匹配：`{"iconKey": "⁉", "iconMatched": false}`

### 2. 标签-活动关联

**问题**：
- 当前 `startBehavior` 只接受 `activityId`，无法通过标签名开始行为
- 用户说"开始小睡"时，系统不知道"小睡"是标签而非活动

**解决方案**：
- 在 `startBehavior` 中添加 `tagName` 可选参数
- 当传入时，查找标签关联的活动
- 如果标签无关联活动，返回错误提示

**标签分组语义**（需加入工具描述）：
> 标签的分组定义了标签的性质：
> - **简单事件**分组内的标签 = 具体事件（如"小睡"、"睡觉"），可直接用于开始行为
> - **标记类**分组（如"重要"、"紧急"）= 属性标记，不用于开始行为
> - 当用户说"开始XX"时，优先在简单事件分组中查找标签

**逻辑**：
```kotlin
// 当 tagName 传入时
val tag = tagRepository.getByName(tagName)
val activityIds = tagRepository.getActivityIdsForTag(tag.id)
if (activityIds.isEmpty()) {
    return Error("标签「$tagName」未关联任何活动，请先关联")
}
activityId = activityIds.first()
```

## 涉及文件

| 文件 | 改动 |
|------|------|
| `core/tools/.../library/BatchCreateTagsTool.kt` | `resolveIconKey` 支持多关键词搜索；返回格式修正 |
| `core/tools/.../library/BatchCreateActivitiesTool.kt` | 同上 |
| `core/tools/.../timing/IconSearchEngine.kt` | 添加 `searchMultipleQueries` 方法 |
| `core/tools/.../timing/StartBehaviorTool.kt` | 添加 `tagName` 参数；标签-活动关联逻辑 |
| `core/data/.../repository/TagRepository.kt` | 可能需要添加 `getByName` 方法 |

## 验收标准

1. 创建标签时，多关键词搜索图标，优先 hi 库
2. 使用 mi/emoji 库时记录到 `icon_search_miss` 表
3. 无匹配时返回 `⁉`，不说"已自动匹配图标"
4. `startBehavior` 支持 `tagName` 参数
5. 标签未关联活动时返回明确错误提示
