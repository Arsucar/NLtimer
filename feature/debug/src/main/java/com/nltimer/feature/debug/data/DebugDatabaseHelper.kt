package com.nltimer.feature.debug.data

import com.nltimer.core.data.database.NLtimerDatabase
import com.nltimer.core.data.database.dao.ActivityDao
import com.nltimer.core.data.database.dao.ActivityGroupDao
import com.nltimer.core.data.database.dao.BehaviorDao
import com.nltimer.core.data.database.dao.TagDao
import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityGroupEntity
import com.nltimer.core.data.database.entity.ActivityTagBindingEntity
import com.nltimer.core.data.database.entity.BehaviorEntity
import com.nltimer.core.data.database.entity.BehaviorTagCrossRefEntity
import com.nltimer.core.data.database.entity.TagEntity
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库调试操作辅助类
 * 封装调试页面所需的数据库清除、插入测试数据、查询全部数据等操作
 */
@Singleton
class DebugDatabaseHelper @Inject constructor(
    private val activityDao: ActivityDao,
    private val activityGroupDao: ActivityGroupDao,
    private val tagDao: TagDao,
    private val behaviorDao: BehaviorDao,
    private val database: NLtimerDatabase,
) {
    // 按外键依赖顺序清除所有表数据
    suspend fun clearAllTables() {
        database.withTransaction {
            behaviorDao.deleteAllTagCrossRefs()
            behaviorDao.deleteAllActivityTagBindings()
            behaviorDao.deleteAll()
            activityDao.deleteAll()
            tagDao.deleteAll()
            activityGroupDao.deleteAll()
        }
    }

    // 为所有表批量插入测试数据，建立关联关系
    suspend fun insertAllTestData() {
        database.withTransaction {
            val groupIdWork = activityGroupDao.insert(ActivityGroupEntity(name = "工作", sortOrder = 0))
            val groupIdLife = activityGroupDao.insert(ActivityGroupEntity(name = "生活", sortOrder = 1))
            val groupIdSport = activityGroupDao.insert(ActivityGroupEntity(name = "运动", sortOrder = 2))

            val activityIdRun = activityDao.insert(ActivityEntity(name = "跑步", iconKey = "🏃", groupId = groupIdSport))
            val activityIdRead = activityDao.insert(ActivityEntity(name = "阅读", iconKey = "📖", groupId = groupIdLife))
            val activityIdCode = activityDao.insert(ActivityEntity(name = "编程", iconKey = "💻", groupId = groupIdWork))
            val activityIdMeeting = activityDao.insert(ActivityEntity(name = "会议", iconKey = "📋", groupId = groupIdWork))
            val activityIdMeditate = activityDao.insert(ActivityEntity(name = "冥想", iconKey = "🧘", groupId = groupIdLife))

            val tagIdImportant = tagDao.insert(TagEntity(name = "重要", color = 0xFFFF4444, category = "优先级", priority = 3))
            val tagIdUrgent = tagDao.insert(TagEntity(name = "紧急", color = 0xFFFF9800, category = "优先级", priority = 2))
            val tagIdDaily = tagDao.insert(TagEntity(name = "日常", color = 0xFF2196F3, category = "类型", priority = 1))
            val tagIdFocus = tagDao.insert(TagEntity(name = "专注", color = 0xFF4CAF50, category = "类型", priority = 1))

            val now = System.currentTimeMillis()
            val behaviorIdRun = behaviorDao.insert(BehaviorEntity(activityId = activityIdRun, startTime = now - 3600000, endTime = now - 1800000, status = "completed", note = "晨跑30分钟"))
            val behaviorIdRead = behaviorDao.insert(BehaviorEntity(activityId = activityIdRead, startTime = now - 7200000, endTime = null, status = "active", note = "正在阅读"))
            val behaviorIdCode = behaviorDao.insert(BehaviorEntity(activityId = activityIdCode, startTime = 0, endTime = null, status = "pending", note = "待开始编程"))

            behaviorDao.insertActivityTagBindings(
                listOf(
                    ActivityTagBindingEntity(activityId = activityIdRun, tagId = tagIdDaily),
                    ActivityTagBindingEntity(activityId = activityIdCode, tagId = tagIdFocus),
                    ActivityTagBindingEntity(activityId = activityIdMeeting, tagId = tagIdUrgent),
                )
            )

            behaviorDao.insertTagCrossRefs(
                listOf(
                    BehaviorTagCrossRefEntity(behaviorId = behaviorIdRun, tagId = tagIdDaily),
                    BehaviorTagCrossRefEntity(behaviorId = behaviorIdRead, tagId = tagIdFocus),
                )
            )
        }
    }

    // 清除指定表数据
    suspend fun clearTable(tableName: String) {
        when (tableName) {
            "activities" -> activityDao.deleteAll()
            "activity_groups" -> activityGroupDao.deleteAll()
            "tags" -> tagDao.deleteAll()
            "behaviors" -> behaviorDao.deleteAll()
            "activity_tag_binding" -> behaviorDao.deleteAllActivityTagBindings()
            "behavior_tag_cross_ref" -> behaviorDao.deleteAllTagCrossRefs()
        }
    }

    // 为指定表插入测试数据
    suspend fun insertTestData(tableName: String) {
        when (tableName) {
            "activity_groups" -> {
                activityGroupDao.insert(ActivityGroupEntity(name = "工作", sortOrder = 0))
                activityGroupDao.insert(ActivityGroupEntity(name = "生活", sortOrder = 1))
                activityGroupDao.insert(ActivityGroupEntity(name = "运动", sortOrder = 2))
            }
            "activities" -> {
                activityDao.insert(ActivityEntity(name = "跑步", iconKey = "🏃"))
                activityDao.insert(ActivityEntity(name = "阅读", iconKey = "📖"))
                activityDao.insert(ActivityEntity(name = "编程", iconKey = "💻"))
                activityDao.insert(ActivityEntity(name = "会议", iconKey = "📋"))
                activityDao.insert(ActivityEntity(name = "冥想", iconKey = "🧘"))
            }
            "tags" -> {
                tagDao.insert(TagEntity(name = "重要", color = 0xFFFF4444, category = "优先级", priority = 3))
                tagDao.insert(TagEntity(name = "紧急", color = 0xFFFF9800, category = "优先级", priority = 2))
                tagDao.insert(TagEntity(name = "日常", color = 0xFF2196F3, category = "类型", priority = 1))
                tagDao.insert(TagEntity(name = "专注", color = 0xFF4CAF50, category = "类型", priority = 1))
            }
            "behaviors" -> {
                val now = System.currentTimeMillis()
                behaviorDao.insert(BehaviorEntity(activityId = 1, startTime = now - 3600000, endTime = now - 1800000, status = "completed", note = "测试行为1"))
                behaviorDao.insert(BehaviorEntity(activityId = 2, startTime = now - 7200000, endTime = null, status = "active", note = "测试行为2"))
                behaviorDao.insert(BehaviorEntity(activityId = 3, startTime = 0, endTime = null, status = "pending", note = "测试行为3"))
            }
            "activity_tag_binding" -> {
                behaviorDao.insertActivityTagBindings(
                    listOf(
                        ActivityTagBindingEntity(activityId = 1, tagId = 1),
                        ActivityTagBindingEntity(activityId = 2, tagId = 2),
                        ActivityTagBindingEntity(activityId = 3, tagId = 3),
                    )
                )
            }
            "behavior_tag_cross_ref" -> {
                behaviorDao.insertTagCrossRefs(
                    listOf(
                        BehaviorTagCrossRefEntity(behaviorId = 1, tagId = 1),
                        BehaviorTagCrossRefEntity(behaviorId = 2, tagId = 2),
                    )
                )
            }
        }
    }

    // 查询所有表数据，返回 表名 → 行字符串列表
    suspend fun queryAllData(): Map<String, List<String>> {
        return mapOf(
            "activities" to activityDao.getAll().first().map { it.toString() },
            "activity_groups" to activityGroupDao.getAll().first().map { it.toString() },
            "tags" to tagDao.getAll().first().map { it.toString() },
            "behaviors" to behaviorDao.getByDayRange(0, Long.MAX_VALUE).first().map { it.toString() },
            "activity_tag_binding" to behaviorDao.getAllActivityTagBindingsSync().map { it.toString() },
            "behavior_tag_cross_ref" to behaviorDao.getAllCrossRefsSync().map { it.toString() },
        )
    }
}
