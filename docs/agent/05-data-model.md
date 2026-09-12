# 数据模型速查

## 表清单

| 表名 | Entity 类 | DAO 类 | 行数估算 |
|------|-----------|--------|---------|
| activities | ActivityEntity | ActivityDao | 用户创建 |
| activity_groups | ActivityGroupEntity | ActivityGroupDao | 少量 |
| tag_groups | TagGroupEntity | TagGroupDao | 少量（新增 v14） |
| tags | TagEntity | TagDao | 用户创建 |
| behaviors | BehaviorEntity | BehaviorDao | 大量（核心数据） |
| activity_tag_binding | ActivityTagBindingEntity | — | 中等 |
| behavior_tag_cross_ref | BehaviorTagCrossRefEntity | — | 大量 |
| icon_search_miss | IconSearchMissEntity | IconSearchMissDao | 少量 |
| event_template | EventTemplateEntity | EventTemplateDao | 少量（v17） |
| event_template_field | EventTemplateFieldEntity | EventTemplateFieldDao | 模板×字段数（v17） |
| event_template_tag_binding | EventTemplateTagBindingEntity | EventTemplateTagBindingDao | 少量（v17） |
| behavior_event | BehaviorEventEntity | BehaviorEventDao | 中等（v17） |
| behavior_event_value | BehaviorEventValueEntity | BehaviorEventValueDao | 大量（v17，EAV） |

## 实体字段

### ActivityEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| name | String | 活动名称 |
| iconKey | String? | 图标标识 |
| keywords | String? | 智能匹配关键词 |
| groupId | Long? (FK) | → activity_groups.id |
| isPreset | Boolean | 是否预置 |
| isArchived | Boolean | 是否归档 |
| archivedAt | Long? | 归档时间 |
| archiveNote | String? | 归档感想（可空） |
| color | Long? | 自定义颜色 |
| usageCount | Int | 使用次数 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 更新时间 |

### ActivityGroupEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| name | String | 分组名称 |
| iconKey | String? | 图标（v14 新增） |
| sortOrder | Int | 排序 |
| isArchived | Boolean | 是否归档 |
| archivedAt | Long? | 归档时间 |
| createdAt | Long | 创建时间 |

### TagGroupEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| name | String | 分组名称（unique index） |
| iconKey | String? | 分组统一图标 |
| sortOrder | Int | 排序 |
| isArchived | Boolean | 是否归档 |
| archivedAt | Long? | 归档时间 |
| createdAt | Long | 创建时间 |

### TagEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| name | String | 标签名称 |
| color | Long? | 颜色 |
| iconKey | String? | 图标 |
| category | String? | 分类（待废弃，逐步迁移到 groupId） |
| groupId | Long? (FK) | → tag_groups.id（v14 新增） |
| priority | Int | 优先级 |
| usageCount | Int | 使用次数 |
| sortOrder | Int | 排序 |
| keywords | String? | 智能匹配关键词 |
| isArchived | Boolean | 是否归档 |
| archivedAt | Long? | 归档时间 |
| archiveNote | String? | 归档感想（可空） |

### BehaviorEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| activityId | Long (FK) | → activities.id |
| startTime | Long | 开始时间 (epoch ms) |
| endTime | Long? | 结束时间 (null=进行中) |
| status | BehaviorNature | PENDING/ACTIVE/COMPLETED |
| note | String? | 备注 |
| pomodoroCount | Int | 番茄钟计数 |
| sequence | Int | 排序（待定列表） |
| estimatedDuration | Long? | 预估时长 (ms) |
| actualDuration | Long? | 实际时长 (ms) |
| achievementLevel | Int? | 成就等级 |
| wasPlanned | Boolean | 是否为计划行为 |

### ActivityTagBindingEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| activityId | Long (FK) | → activities.id |
| tagId | Long (FK) | → tags.id |
| source | String | 绑定方向：`activity` / `tag` |

### BehaviorTagCrossRefEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| behaviorId | Long (FK) | → behaviors.id |
| tagId | Long (FK) | → tags.id |

### IconSearchMissEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| query | String | 搜索关键词 |
| library | String | 图标库过滤 |
| timestamp | Long | 记录时间 |

### EventTemplateEntity

> v17 新增。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| name | String | 模板名称 |
| description | String? | 描述 |
| createdAt | Long | 创建时间 |
| sortOrder | Int | 排序 |

### EventTemplateFieldEntity

> v17；FK→event_template CASCADE。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| templateId | Long (FK) | → event_template.id |
| name | String | 字段名 |
| type | String | EventFieldType key：`select`/`text`/`number`/`rating` |
| optionsJson | String? | 单选选项 JSON 数组（kotlinx 序列化 String? 列） |
| sortOrder | Int | 排序 |

### EventTemplateTagBindingEntity

> v17；联合主键 templateId+tagId，双 FK CASCADE。

| 字段 | 类型 | 说明 |
|------|------|------|
| templateId | Long (PK, FK) | → event_template.id |
| tagId | Long (PK, FK) | → tags.id |

### BehaviorEventEntity

> v17；FK→event_template CASCADE；behaviorId 可空=独立事件，非空时 FK→behaviors CASCADE。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| behaviorId | Long? (FK) | → behaviors.id；null=独立事件 |
| activityId | Long? | 冗余（冗余索引直查，无 FK） |
| templateId | Long (FK) | → event_template.id |
| timestamp | Long | 事件时间 (epoch ms) |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 更新时间 |

### BehaviorEventValueEntity

> v17 EAV 行；FK→behavior_event / event_template_field 均 CASCADE。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| eventId | Long (FK) | → behavior_event.id |
| fieldId | Long (FK) | → event_template_field.id |
| valueText | String? | 文本/单选值 |
| valueNumber | Double? | 数值/星级值 |

### 关系表

| 表 | 字段 | 关系 |
|----|------|------|
| activity_tag_binding | activityId (FK), tagId (FK) | Activity ↔ Tag (M:N) |
| behavior_tag_cross_ref | behaviorId (FK), tagId (FK) | Behavior ↔ Tag (M:N) |

## BehaviorNature 枚举

| 值 | 符号 | 说明 |
|----|------|------|
| PENDING | ○ | 计划中 |
| ACTIVE | ▶ | 进行中 |
| COMPLETED | ✓ | 已完成 |

## EventFieldType 枚举

| key | 说明 | 值列 |
|-----|------|------|
| select | 单选（选项在 optionsJson） | valueText |
| text | 长文本 | valueText |
| number | 数值 | valueNumber |
| rating | 星级 1~5 | valueNumber |

## EventQueryScope（事件查询范围）

| 分支 | 说明 |
|------|------|
| All | 全局混排（timestamp DESC） |
| ByActivity(activityId) | 按活动 |
| ByTag(tagId) | 经 behavior→tag 或绑定表 JOIN |
| ByBehavior(behaviorId) | 单行为事件 |
| ByTemplate(templateId) | 单模板聚焦 |

## 关系图

```
TagGroup 1:N Tag
ActivityGroup 1:N Activity M:N Tag
                        |
                        1:N
                    Behavior M:N Tag
                         |
                         1:N (behaviorId 可空)
                     BehaviorEvent 1:N BehaviorEventValue
                         |                       |
                         N:1                  N:1
                  EventTemplate 1:N EventTemplateField
                         N:M Tag (event_template_tag_binding)

## DB 版本历史

| 版本 | 变更 |
|------|------|
| 13 | v0.1.5 |
| 14 | 新增 tag_groups 表；TagEntity 新增 groupId；ActivityGroupEntity 新增 iconKey |
| 15 | activity_tag_binding 新增 source |
| 16 | activities/tags 新增 archiveNote |
| 17 | 事件记录器：新增 event_template / event_template_field / event_template_tag_binding / behavior_event / behavior_event_value 五表 |

## DisplayColorConfig

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| activityIconColorMode | DisplayColorMode | NORMAL | 活动图标颜色模式 |
| tagDisplayColorMode | DisplayColorMode | NORMAL | 标签颜色模式 |
| showTagIcon | Boolean | true | 全局标签图标显示开关（v14 新增） |
```
