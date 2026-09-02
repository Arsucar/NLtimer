package com.nltimer.core.data.model

import com.nltimer.core.data.database.entity.ActivityEntity
import com.nltimer.core.data.database.entity.ActivityGroupEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelConversionTest {

    // --- Activity <-> ActivityEntity ---

    @Test
    fun `Activity toEntity maps all fields`() {
        val activity = Activity(
            id = 42, name = "测试活动", iconKey = "icon", keywords = "关键词",
            groupId = 5L, isPreset = true, isArchived = false, archivedAt = null,
            archiveNote = "看完了", color = 0xFF0000, usageCount = 10,
        )

        val entity = activity.toEntity()

        assertEquals(42L, entity.id)
        assertEquals("测试活动", entity.name)
        assertEquals("icon", entity.iconKey)
        assertEquals("关键词", entity.keywords)
        assertEquals(5L, entity.groupId)
        assertTrue(entity.isPreset)
        assertEquals(false, entity.isArchived)
        assertNull(entity.archivedAt)
        assertEquals("看完了", entity.archiveNote)
        assertEquals(0xFF0000L, entity.color)
        assertEquals(10, entity.usageCount)
    }

    @Test
    fun `Activity fromEntity maps all fields`() {
        val entity = ActivityEntity(
            id = 7, name = "实体活动", iconKey = "emoji", keywords = "key",
            groupId = 3L, isPreset = false, isArchived = true, archivedAt = 1000L,
            archiveNote = "阶段性结束", color = 0x00FF00, usageCount = 5,
        )

        val activity = Activity.fromEntity(entity)

        assertEquals(7L, activity.id)
        assertEquals("实体活动", activity.name)
        assertEquals("emoji", activity.iconKey)
        assertEquals("key", activity.keywords)
        assertEquals(3L, activity.groupId)
        assertEquals(false, activity.isPreset)
        assertTrue(activity.isArchived)
        assertEquals(1000L, activity.archivedAt)
        assertEquals("阶段性结束", activity.archiveNote)
        assertEquals(0x00FF00L, activity.color)
        assertEquals(5, activity.usageCount)
    }

    @Test
    fun `Activity toEntity and fromEntity roundtrip`() {
        val original = Activity(
            id = 1, name = "往返测试", iconKey = null, keywords = null,
            groupId = null, isPreset = false, isArchived = true, archivedAt = 999L,
            archiveNote = "可空感想", color = null, usageCount = 0,
        )

        val entity = original.toEntity()
        val restored = Activity.fromEntity(entity)

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.iconKey, restored.iconKey)
        assertEquals(original.keywords, restored.keywords)
        assertEquals(original.groupId, restored.groupId)
        assertEquals(original.isPreset, restored.isPreset)
        assertEquals(original.isArchived, restored.isArchived)
        assertEquals(original.archivedAt, restored.archivedAt)
        assertEquals(original.archiveNote, restored.archiveNote)
        assertEquals(original.color, restored.color)
        assertEquals(original.usageCount, restored.usageCount)
    }

    @Test
    fun `Activity default values`() {
        val activity = Activity(name = "默认值测试")

        assertEquals(0L, activity.id)
        assertNull(activity.iconKey)
        assertNull(activity.keywords)
        assertNull(activity.groupId)
        assertEquals(false, activity.isPreset)
        assertEquals(false, activity.isArchived)
        assertNull(activity.archivedAt)
        assertNull(activity.archiveNote)
        assertNull(activity.color)
        assertEquals(0, activity.usageCount)
    }

    @Test
    fun `Activity with null fields maps correctly`() {
        val activity = Activity(
            id = 1, name = "空值测试",
            iconKey = null, keywords = null, groupId = null,
            color = null, archivedAt = null, archiveNote = null,
        )

        val entity = activity.toEntity()

        assertNull(entity.iconKey)
        assertNull(entity.keywords)
        assertNull(entity.groupId)
        assertNull(entity.color)
        assertNull(entity.archivedAt)
        assertNull(entity.archiveNote)
    }

    // --- ActivityGroup <-> ActivityGroupEntity ---

    @Test
    fun `ActivityGroup toEntity maps all fields`() {
        val group = ActivityGroup(
            id = 10, name = "测试分组", sortOrder = 3,
            isArchived = true, archivedAt = 500L,
        )

        val entity = group.toEntity()

        assertEquals(10L, entity.id)
        assertEquals("测试分组", entity.name)
        assertEquals(3, entity.sortOrder)
        assertTrue(entity.isArchived)
        assertEquals(500L, entity.archivedAt)
    }

    @Test
    fun `ActivityGroup fromEntity maps all fields`() {
        val entity = ActivityGroupEntity(
            id = 5, name = "实体分组", sortOrder = 7,
            isArchived = false, archivedAt = null,
        )

        val group = ActivityGroup.fromEntity(entity)

        assertEquals(5L, group.id)
        assertEquals("实体分组", group.name)
        assertEquals(7, group.sortOrder)
        assertEquals(false, group.isArchived)
        assertNull(group.archivedAt)
    }

    @Test
    fun `ActivityGroup toEntity and fromEntity roundtrip`() {
        val original = ActivityGroup(
            id = 99, name = "往返分组", sortOrder = 2,
            isArchived = true, archivedAt = 12345L,
        )

        val entity = original.toEntity()
        val restored = ActivityGroup.fromEntity(entity)

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.sortOrder, restored.sortOrder)
        assertEquals(original.isArchived, restored.isArchived)
        assertEquals(original.archivedAt, restored.archivedAt)
    }

    @Test
    fun `ActivityGroup default values`() {
        val group = ActivityGroup(name = "默认分组")

        assertEquals(0L, group.id)
        assertEquals(0, group.sortOrder)
        assertEquals(false, group.isArchived)
        assertNull(group.archivedAt)
    }

    // --- BehaviorNature ---

    @Test
    fun `BehaviorNature key values are correct`() {
        assertEquals("pending", BehaviorNature.PENDING.key)
        assertEquals("active", BehaviorNature.ACTIVE.key)
        assertEquals("completed", BehaviorNature.COMPLETED.key)
    }

    @Test
    fun `BehaviorNature has exactly 3 values`() {
        assertEquals(3, BehaviorNature.entries.size)
    }

    @Test
    fun `BehaviorNature can be found by key via fromKey`() {
        assertEquals(BehaviorNature.PENDING, BehaviorNature.fromKey("pending"))
        assertEquals(BehaviorNature.ACTIVE, BehaviorNature.fromKey("active"))
        assertEquals(BehaviorNature.COMPLETED, BehaviorNature.fromKey("completed"))
    }

    @Test
    fun `BehaviorNature unknown key returns PENDING fallback`() {
        assertEquals(BehaviorNature.PENDING, BehaviorNature.fromKey("unknown"))
    }

    // --- ActivityStats ---

    @Test
    fun `ActivityStats default values`() {
        val stats = ActivityStats()

        assertEquals(0, stats.usageCount)
        assertEquals(0L, stats.totalDurationMinutes)
        assertNull(stats.lastUsedTimestamp)
    }

    @Test
    fun `ActivityStats with custom values`() {
        val stats = ActivityStats(usageCount = 42, totalDurationMinutes = 120, lastUsedTimestamp = 9999L)

        assertEquals(42, stats.usageCount)
        assertEquals(120L, stats.totalDurationMinutes)
        assertEquals(9999L, stats.lastUsedTimestamp)
    }

    // --- Behavior data class ---

    @Test
    fun `Behavior data class equality`() {
        val b1 = Behavior(
            id = 1, activityId = 1, startTime = 1000, endTime = 2000,
            status = BehaviorNature.COMPLETED, note = "备注", pomodoroCount = 1,
            sequence = 0, estimatedDuration = 60L, actualDuration = 55L,
            achievementLevel = 90, wasPlanned = true,
        )
        val b2 = b1.copy()

        assertEquals(b1, b2)
        assertEquals(b1.hashCode(), b2.hashCode())
    }

    @Test
    fun `Behavior with different status not equal`() {
        val base = Behavior(
            id = 1, activityId = 1, startTime = 1000, endTime = null,
            status = BehaviorNature.ACTIVE, note = null, pomodoroCount = 0,
            sequence = 0, estimatedDuration = null, actualDuration = null,
            achievementLevel = null, wasPlanned = false,
        )
        val different = base.copy(status = BehaviorNature.COMPLETED)

        assertTrue(base != different)
    }

    // --- Tag data class ---

    @Test
    fun `Tag data class equality`() {
        val t1 = Tag(
            id = 1, name = "标签", color = 0xFF0000, iconKey = null,
            category = "分类", groupId = null, priority = 1, usageCount = 5,
            sortOrder = 0, keywords = null, isArchived = false, archivedAt = null,
            archiveNote = "看完后的想法",
        )
        val t2 = t1.copy()

        assertEquals(t1, t2)
    }

    @Test
    fun `Tag toEntity and fromEntity roundtrip archiveNote`() {
        val original = Tag(
            id = 9, name = "轻小说", color = null, iconKey = null,
            category = "文学", groupId = null, priority = 0, usageCount = 0,
            sortOrder = 0, keywords = null, isArchived = true, archivedAt = 2000L,
            archiveNote = "这本书看完了",
        )

        val restored = Tag.fromEntity(original.toEntity())

        assertEquals(original.archiveNote, restored.archiveNote)
        assertEquals(original.archivedAt, restored.archivedAt)
        assertEquals(original.isArchived, restored.isArchived)
    }

    @Test
    fun `Tag archiveNote defaults to null`() {
        val tag = Tag(
            id = 1, name = "无感想", color = null, iconKey = null,
            category = null, groupId = null, priority = 0, usageCount = 0,
            sortOrder = 0, keywords = null, isArchived = true, archivedAt = 1L,
        )
        assertNull(tag.archiveNote)
        assertNull(tag.toEntity().archiveNote)
    }

    // --- BehaviorWithDetails ---

    @Test
    fun `BehaviorWithDetails aggregates correctly`() {
        val behavior = Behavior(
            id = 1, activityId = 10, startTime = 1000, endTime = 2000,
            status = BehaviorNature.COMPLETED, note = null, pomodoroCount = 0,
            sequence = 0, estimatedDuration = null, actualDuration = null,
            achievementLevel = null, wasPlanned = false,
        )
        val activity = Activity(id = 10, name = "活动")
        val tags = listOf(
            Tag(1, "标签1", null, null, null, null, 0, 0, 0, null, false),
            Tag(2, "标签2", null, null, null, null, 0, 0, 0, null, false),
        )

        val details = BehaviorWithDetails(behavior = behavior, activity = activity, tags = tags)

        assertEquals(1L, details.behavior.id)
        assertEquals("活动", details.activity.name)
        assertEquals(2, details.tags.size)
    }
}
