package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable
import com.nltimer.core.data.database.entity.EventTemplateFieldEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 事件打点模板字段类型
 * 与 BehaviorNature 同型：TEXT key 存库（无 TypeConverter），未知名回退默认值
 */
enum class EventFieldType(val key: String) {
    SELECT("select"),
    TEXT("text"),
    NUMBER("number"),
    RATING("rating");

    companion object {
        private val keyMap = entries.associateBy { it.key }
        fun fromKey(key: String): EventFieldType = keyMap[key] ?: TEXT
    }
}

/** optionsJson 编解码：列表非空存 JSON 数组字符串，空列表存 null */
private val optionsJsonFormat = Json { ignoreUnknownKeys = true }

private fun decodeOptions(optionsJson: String?): List<String> =
    optionsJson?.let { json ->
        runCatching { optionsJsonFormat.decodeFromString<List<String>>(json) }
            .getOrDefault(emptyList())
    } ?: emptyList()

private fun encodeOptions(options: List<String>): String? =
    if (options.isEmpty()) null else optionsJsonFormat.encodeToString(options)

/**
 * EventTemplateField 事件打点模板字段领域模型
 * 单选字段（SELECT）的选项存储为 [options]，库内为 optionsJson 列的 JSON 数组字符串
 */
@Immutable
data class EventTemplateField(
    val id: Long = 0,
    val templateId: Long = 0,
    val name: String,
    val type: EventFieldType,
    val options: List<String> = emptyList(),
    val sortOrder: Int = 0,
) {
    fun toEntity() = EventTemplateFieldEntity(
        id = id,
        templateId = templateId,
        name = name,
        type = type.key,
        optionsJson = encodeOptions(options),
        sortOrder = sortOrder,
    )

    companion object {
        fun fromEntity(entity: EventTemplateFieldEntity) = EventTemplateField(
            id = entity.id,
            templateId = entity.templateId,
            name = entity.name,
            type = EventFieldType.fromKey(entity.type),
            options = decodeOptions(entity.optionsJson),
            sortOrder = entity.sortOrder,
        )
    }
}
