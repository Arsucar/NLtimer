package com.nltimer.core.data.mock

import com.nltimer.core.data.model.*

object MockData {
    val mockActivity = Activity(
        id = 1L,
        name = "测试活动",
        iconKey = "📝",
        groupId = 1L,
        color = 0xFFFF0000,
        isArchived = false,
    )

    val mockTag = Tag(
        id = 1L,
        name = "测试标签",
        color = 0xFF00FF00,
        iconKey = null,
        category = "工作",
        groupId = null,
        priority = 1,
        usageCount = 0,
        sortOrder = 0,
        keywords = null,
        isArchived = false,
    )

    val mockBehavior = Behavior(
        id = 1L,
        activityId = 1L,
        startTime = System.currentTimeMillis(),
        endTime = System.currentTimeMillis() + 3600000,
        status = BehaviorNature.COMPLETED,
        note = "测试备注",
        pomodoroCount = 1,
        sequence = 1,
        estimatedDuration = 3600000L,
        actualDuration = 3600000L,
        achievementLevel = 5,
        wasPlanned = true,
    )

    val mockActivityGroup = ActivityGroup(
        id = 1L,
        name = "测试分组",
        sortOrder = 0,
    )
}