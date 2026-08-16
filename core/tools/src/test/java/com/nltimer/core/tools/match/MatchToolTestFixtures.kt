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

/**
 * 工具成功结果统一返回 JSON 字符串（见 docs/agent/06-common-bug.md #2），
 * 测试辅助把 JSON 解析回 Map，保持既有断言风格不变。
 */
internal fun ToolResult.successData(): Map<String, Any> {
    assertTrue(this is ToolResult.Success)
    val data = (this as ToolResult.Success).data as String
    return jsonObjectToMap(org.json.JSONObject(data))
}

internal fun Map<String, Any>.activityRows(): List<Map<String, Any>> = rows("activities")

internal fun Map<String, Any>.tagRows(): List<Map<String, Any>> = rows("tags")

private fun Map<String, Any>.rows(key: String): List<Map<String, Any>> {
    @Suppress("UNCHECKED_CAST")
    return this[key] as List<Map<String, Any>>
}

private fun jsonObjectToMap(obj: org.json.JSONObject): Map<String, Any> {
    val map = LinkedHashMap<String, Any>()
    obj.keys().forEach { key ->
        jsonValueToKotlin(obj.get(key))?.let { map[key] = it }
    }
    return map
}

private fun jsonArrayToList(arr: org.json.JSONArray): List<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until arr.length()) {
        list.add(jsonValueToKotlin(arr.get(i)))
    }
    return list
}

private fun jsonValueToKotlin(value: Any?): Any? {
    if (value == null || value === org.json.JSONObject.NULL) return null
    return when (value) {
        is org.json.JSONObject -> jsonObjectToMap(value)
        is org.json.JSONArray -> jsonArrayToList(value)
        is Boolean -> value
        is Number -> if (value.toDouble() % 1.0 == 0.0) value.toLong() else value.toDouble()
        else -> value.toString()
    }
}
