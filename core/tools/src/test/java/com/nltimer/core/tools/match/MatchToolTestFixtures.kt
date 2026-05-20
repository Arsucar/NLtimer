package com.nltimer.core.tools.match

import com.nltimer.core.data.model.Tag
import com.nltimer.core.tools.ToolResult
import org.junit.Assert.assertTrue

internal fun tagFixture(
    id: Long,
    name: String,
    keywords: String? = null,
    isArchived: Boolean = false,
) = Tag(
    id = id,
    name = name,
    color = null,
    iconKey = null,
    category = null,
    groupId = null,
    priority = 0,
    usageCount = 0,
    sortOrder = 0,
    isArchived = isArchived,
    archivedAt = if (isArchived) 0L else null,
    keywords = keywords,
)

internal fun ToolResult.successData(): Map<String, Any> {
    assertTrue(this is ToolResult.Success)
    @Suppress("UNCHECKED_CAST")
    return (this as ToolResult.Success).data as Map<String, Any>
}

internal fun Map<String, Any>.activityRows(): List<Map<String, Any>> = rows("activities")

internal fun Map<String, Any>.tagRows(): List<Map<String, Any>> = rows("tags")

private fun Map<String, Any>.rows(key: String): List<Map<String, Any>> {
    @Suppress("UNCHECKED_CAST")
    return this[key] as List<Map<String, Any>>
}
