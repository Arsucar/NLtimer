package com.nltimer.core.debugui

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.Tag

/**
 * 字段信息数据类，用于描述单个字段的调试信息。
 */
data class FieldInfo(
    val name: String,
    val displayName: String,
    val value: Any?,
    val isDisplayed: Boolean,
    val isMissing: Boolean,
)

fun Any?.isFieldMissing(fieldName: String): Boolean = when (this) {
    null -> true
    is String -> isBlank()
    is Int -> this == 0 && !fieldName.endsWith("Id")
    is Long -> this == 0L && !fieldName.endsWith("Id")
    is Boolean -> !this
    else -> false
}

fun Activity.toFieldInfoList(): List<FieldInfo> = listOf(
    FieldInfo("id", "ID", id, isDisplayed = false, isMissing = id == 0L),
    FieldInfo("name", "名称", name, isDisplayed = true, isMissing = name.isFieldMissing("name")),
    FieldInfo("iconKey", "图标", iconKey, isDisplayed = true, isMissing = iconKey.isFieldMissing("iconKey")),
    FieldInfo("color", "颜色", color, isDisplayed = true, isMissing = color.isFieldMissing("color")),
    FieldInfo("keywords", "关键词", keywords, isDisplayed = false, isMissing = keywords.isFieldMissing("keywords")),
    FieldInfo("groupId", "分组", groupId, isDisplayed = false, isMissing = groupId.isFieldMissing("groupId")),
    FieldInfo("isPreset", "预设", isPreset, isDisplayed = false, isMissing = false),
    FieldInfo("isArchived", "归档", isArchived, isDisplayed = false, isMissing = false),
    FieldInfo("archivedAt", "归档时间", archivedAt, isDisplayed = false, isMissing = archivedAt.isFieldMissing("archivedAt")),
    FieldInfo("archiveNote", "归档感想", archiveNote, isDisplayed = false, isMissing = archiveNote.isFieldMissing("archiveNote")),
    FieldInfo("usageCount", "使用次数", usageCount, isDisplayed = true, isMissing = usageCount.isFieldMissing("usageCount")),
)

fun Tag.toFieldInfoList(): List<FieldInfo> = listOf(
    FieldInfo("id", "ID", id, isDisplayed = false, isMissing = id == 0L),
    FieldInfo("name", "名称", name, isDisplayed = true, isMissing = name.isFieldMissing("name")),
    FieldInfo("color", "颜色", color, isDisplayed = true, isMissing = color.isFieldMissing("color")),
    FieldInfo("iconKey", "图标", iconKey, isDisplayed = true, isMissing = iconKey.isFieldMissing("iconKey")),
    FieldInfo("category", "分类", category, isDisplayed = false, isMissing = category.isFieldMissing("category")),
    FieldInfo("priority", "优先级", priority, isDisplayed = false, isMissing = priority.isFieldMissing("priority")),
    FieldInfo("usageCount", "使用次数", usageCount, isDisplayed = true, isMissing = usageCount.isFieldMissing("usageCount")),
    FieldInfo("sortOrder", "排序", sortOrder, isDisplayed = false, isMissing = false),
    FieldInfo("keywords", "关键词", keywords, isDisplayed = false, isMissing = keywords.isFieldMissing("keywords")),
    FieldInfo("isArchived", "归档", isArchived, isDisplayed = false, isMissing = false),
    FieldInfo("archivedAt", "归档时间", archivedAt, isDisplayed = false, isMissing = archivedAt.isFieldMissing("archivedAt")),
    FieldInfo("archiveNote", "归档感想", archiveNote, isDisplayed = false, isMissing = archiveNote.isFieldMissing("archiveNote")),
)

fun List<FieldInfo>.toJsonString(): String = buildString {
    append("{\n")
    this@toJsonString.forEachIndexed { index, field ->
        val valueStr = when (val value = field.value) {
            null -> "null"
            is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
            is Boolean -> value.toString()
            is Number -> value.toString()
            else -> "\"$value\""
        }
        append("  \"${field.name}\": $valueStr")
        if (index < this@toJsonString.size - 1) append(",")
        append("\n")
    }
    append("}")
}
