package com.nltimer.app.experimental.ai_inter.network

import com.nltimer.core.tools.ParameterType
import com.nltimer.core.tools.ToolDefinition
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 把 [ToolDefinition] 转成 OpenAI Function Calling 协议中的 tools 数组单项。
 * 形如 {"type":"function","function":{"name":...,"description":...,"parameters":{...}}}。
 */
internal fun ToolDefinition.toOpenAiFunctionJson(): JsonObject = buildJsonObject {
    put("type", "function")
    putJsonObject("function") {
        put("name", name)
        put("description", description)
        putJsonObject("parameters") {
            put("type", "object")
            putJsonObject("properties") {
                parameters.forEach { p ->
                    putJsonObject(p.name) {
                        put("type", p.type.toJsonSchemaType())
                        put("description", p.description)
                        p.constraints?.enum?.let { enumValues ->
                            putJsonArray("enum") { enumValues.forEach { add(it) } }
                        }
                    }
                }
            }
            val required = parameters.filter { it.required }.map { it.name }
            if (required.isNotEmpty()) {
                putJsonArray("required") { required.forEach { add(it) } }
            }
        }
    }
}

private fun ParameterType.toJsonSchemaType(): String = when (this) {
    ParameterType.STRING, ParameterType.DATE_TIME, ParameterType.DURATION -> "string"
    ParameterType.NUMBER -> "number"
    ParameterType.BOOLEAN -> "boolean"
    ParameterType.ARRAY -> "array"
    ParameterType.OBJECT -> "object"
}
