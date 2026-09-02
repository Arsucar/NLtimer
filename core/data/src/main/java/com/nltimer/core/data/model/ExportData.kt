package com.nltimer.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExportData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val activities: List<ExportedActivity>? = null,
    val activityGroups: List<ExportedActivityGroup>? = null,
    val tags: List<ExportedTag>? = null,
    val tagCategories: List<String>? = null,
)

@Serializable
data class ExportedActivity(
    val name: String,
    val iconKey: String? = null,
    val keywords: String? = null,
    val groupName: String? = null,
    val isPreset: Boolean = false,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveNote: String? = null,
    val color: Long? = null,
    val usageCount: Int = 0,
    val tagNames: List<String> = emptyList(),
)

@Serializable
data class ExportedActivityGroup(
    val name: String,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
)

@Serializable
data class ExportedTag(
    val name: String,
    val color: Long? = null,
    val iconKey: String? = null,
    val category: String? = null,
    val priority: Int = 0,
    val usageCount: Int = 0,
    val sortOrder: Int = 0,
    val keywords: String? = null,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveNote: String? = null,
)
