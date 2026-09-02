package com.nltimer.core.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun roundTripSerializationPreservesAllFields() {
        val data = ExportData(
            version = 1,
            exportedAt = 1715299200000L,
            activities = listOf(
                ExportedActivity(
                    name = "阅读",
                    iconKey = "book",
                    groupName = "学习",
                    color = 4280391411,
                    usageCount = 15,
                    tagNames = listOf("专注"),
                    archiveNote = "阶段性结束",
                ),
            ),
            activityGroups = listOf(
                ExportedActivityGroup(name = "学习", sortOrder = 0),
            ),
            tags = listOf(
                ExportedTag(name = "专注", category = "状态", priority = 1, archiveNote = "看完了"),
            ),
            tagCategories = listOf("状态", "场景"),
        )
        val encoded = json.encodeToString(ExportData.serializer(), data)
        val decoded = json.decodeFromString(ExportData.serializer(), encoded)
        assertEquals(data, decoded)
    }

    @Test
    fun partialExportOnlyContainsNonNullFields() {
        val data = ExportData(
            activities = listOf(ExportedActivity(name = "跑步")),
        )
        val encoded = json.encodeToString(ExportData.serializer(), data)
        val decoded = json.decodeFromString(ExportData.serializer(), encoded)
        assertEquals(null, decoded.tags)
        assertEquals(null, decoded.activityGroups)
        assertEquals(1, decoded.activities?.size)
    }
}
