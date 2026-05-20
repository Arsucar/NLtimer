# 数据模型速查

## 表清单

| 表名 | Entity 类 | DAO 类 | 行数估算 |
|------|-----------|--------|---------|
| activities | ActivityEntity | ActivityDao | 用户创建 |
| activity_groups | ActivityGroupEntity | ActivityGroupDao | 少量 |
| tags | TagEntity | TagDao | 用户创建 |
| behaviors | BehaviorEntity | BehaviorDao | 大量（核心数据） |
| activity_tag_binding | ActivityTagBindingEntity | — | 中等 |
| behavior_tag_cross_ref | BehaviorTagCrossRefEntity | — | 大量 |
| icon_search_miss | IconSearchMissEntity | IconSearchMissDao | 少量 |

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
| color | Long? | 自定义颜色 |
| usageCount | Int | 使用次数 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 更新时间 |

### ActivityGroupEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK, auto) | 主键 |
| name | String | 分组名称 |
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
| category | String? | 分类 |
| priority | Int | 优先级 |
| usageCount | Int | 使用次数 |
| sortOrder | Int | 排序 |
| keywords | String? | 智能匹配关键词 |
| isArchived | Boolean | 是否归档 |
| archivedAt | Long? | 归档时间 |

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

## 关系图

```
ActivityGroup 1:N Activity M:N Tag
                       |
                       1:N
                    Behavior M:N Tag
```
