package com.nltimer.core.data.mock

import com.nltimer.core.data.model.*
import kotlin.random.Random

object TestDataFactory {
    fun createActivity(
        id: Long = Random.nextLong(),
        name: String = "活动_${Random.nextInt()}",
        iconKey: String = "📝",
        groupId: Long = 1L,
        color: Long = Random.nextLong(0xFFFFFF),
        isArchived: Boolean = false,
    ) = Activity(id, name, iconKey, null, groupId, false, isArchived, color)

    fun createTag(
        id: Long = Random.nextLong(),
        name: String = "标签_${Random.nextInt()}",
        color: Long = Random.nextLong(0xFFFFFF),
        iconKey: String? = null,
        category: String = "分类_${Random.nextInt(10)}",
        priority: Int = Random.nextInt(10),
        usageCount: Int = 0,
        sortOrder: Int = 0,
        isArchived: Boolean = false,
    ) = Tag(id, name, color, iconKey, category, null, priority, usageCount, sortOrder, null, isArchived)

    fun createBehavior(
        id: Long = Random.nextLong(),
        activityId: Long = 1L,
        startTime: Long = System.currentTimeMillis(),
        endTime: Long = startTime + 3600000,
        status: BehaviorNature = BehaviorNature.COMPLETED,
        note: String = "备注_${Random.nextInt()}",
        pomodoroCount: Int = Random.nextInt(5),
        sequence: Int = 1,
        estimatedDuration: Long? = 3600000L,
        actualDuration: Long? = 3600000L,
        achievementLevel: Int? = 5,
        wasPlanned: Boolean = true,
    ) = Behavior(id, activityId, startTime, endTime, status, note, pomodoroCount, sequence, estimatedDuration, actualDuration, achievementLevel, wasPlanned)

    fun createActivityGroup(
        id: Long = Random.nextLong(),
        name: String = "分组_${Random.nextInt()}",
        sortOrder: Int = 0,
    ) = ActivityGroup(id = id, name = name, sortOrder = sortOrder)

    fun createActivityList(size: Int = 5) = (1..size).map { createActivity(id = it.toLong()) }
    fun createTagList(size: Int = 5) = (1..size).map { createTag(id = it.toLong()) }
    fun createBehaviorList(size: Int = 5) = (1..size).map { createBehavior(id = it.toLong()) }
    fun createActivityGroupList(size: Int = 3) = (1..size).map { createActivityGroup(id = it.toLong()) }
}