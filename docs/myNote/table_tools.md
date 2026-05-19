
共 **26 个工具**，按分类：

### TIMING（8 个）
| 工具 | 功能 | 权限 |
|------|------|------|
| `queryCurrentBehavior` | 查询当前计时 | READ |
| `startBehavior` | 开始计时 | WRITE |
| `endBehavior` | 结束计时 | WRITE |
| `recordBehavior` | 补录行为（含冲突检测） | WRITE |
| `createGoal` | 创建待办目标 | WRITE |
| `deleteBehavior` | 删除单条行为 | FULL |
| `listBehaviors` | 按时间范围查询行为 | READ |
| `getDailySummary` | 每日统计汇总 | READ |

### ACTIVITIES（18 个）
| 工具 | 功能 | 权限 | 批量? |
|------|------|------|-------|
| `listActivities` | 列出所有活动 | READ | — |
| `listTags` | 列出标签 | READ | — |
| `createActivityCategory` | 创建活动分类 | WRITE | — |
| `createTagCategory` | 声明标签分类 | WRITE | — |
| `createActivity` | 创建活动 | WRITE | — |
| `createTag` | 创建标签 | WRITE | — |
| `batchCreateActivities` | 批量创建活动 | WRITE | ✅ |
| `batchCreateTags` | 批量创建标签 | WRITE | ✅ |
| `batchCreateActivityCategories` | 批量创建活动分类 | WRITE | ✅ |
| `batchCreateTagCategories` | 批量声明标签分类 | WRITE | ✅ |
| `batchDeleteBehaviors` | 批量删除行为 | FULL | ✅ |
| `batchDeleteActivities` | 批量删除活动 | FULL | ✅ |
| `bulkUpdateActivities` | 批量更新活动属性 | WRITE | ✅ |
| `setBehaviorTag` | 管理行为标签关联 | FULL | — |
| `exportData` | 按日期范围导出 | READ | — |
| `importData` | 导入数据 | WRITE | — |
| `selectActivitiesAndTags` | 精确匹配搜索 | READ | — |
| `searchActivitiesAndTags` | 子串模糊搜索 | READ | — |

### 空分类
- **STATISTICS** / **GOALS** / **REMINDERS** / **SETTINGS** — 暂无工具

批量工具统一 `MAX_BATCH_SIZE = 20`，删除类操作使用 `FULL` 权限。

---

