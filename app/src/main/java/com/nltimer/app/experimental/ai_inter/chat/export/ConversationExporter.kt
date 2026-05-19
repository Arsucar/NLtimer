package com.nltimer.app.experimental.ai_inter.chat.export

import com.nltimer.app.experimental.ai_inter.chat.data.ConversationEntity
import com.nltimer.app.experimental.ai_inter.chat.data.ConversationMessageEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ConversationExporter @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")

    private fun Long.toLocalDateTime() =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun exportMarkdown(
        conversation: ConversationEntity,
        messages: List<ConversationMessageEntity>,
        options: ExportOptions,
    ): String = buildString {
        appendLine("# ${conversation.title}")
        appendLine()
        appendLine("- 创建时间：${dateFmt.format(conversation.createdAt.toLocalDateTime())}")
        appendLine("- 导出时间：${dateFmt.format(System.currentTimeMillis().toLocalDateTime())}")
        appendLine("- 消息数：${messages.size}")
        appendLine()
        appendLine("---")
        appendLine()

        messages.forEach { msg ->
            val roleLabel = if (msg.role == "user") "用户" else "助手"
            appendLine("## $roleLabel · ${timeFmt.format(msg.createdAt.toLocalDateTime())}")
            appendLine()

            if (msg.role == "assistant") {
                if (options.includeReasoning && msg.reasoning.isNotBlank()) {
                    appendLine("<details><summary>思考过程</summary>")
                    appendLine()
                    appendLine(msg.reasoning)
                    appendLine()
                    appendLine("</details>")
                    appendLine()
                }

                if (options.includeTools && msg.toolCallsJson.isNotBlank()) {
                    val calls = runCatching {
                        json.parseToJsonElement(msg.toolCallsJson).jsonArray
                    }.getOrNull()
                    if (calls != null && calls.isNotEmpty()) {
                        appendLine("<details><summary>工具调用 (${calls.size})</summary>")
                        appendLine()
                        calls.forEachIndexed { idx, callEl ->
                            val call = callEl.jsonObject
                            val name = call["name"]?.jsonPrimitive?.content.orEmpty()
                            val args = call["arguments"]?.jsonPrimitive?.content.orEmpty()
                            val result = call["result"]?.jsonPrimitive?.content.orEmpty()
                            val success = call["success"]?.jsonPrimitive?.content == "true"
                            val duration = call["durationMs"]?.jsonPrimitive?.content.orEmpty()

                            appendLine("### ${idx + 1}. $name · ${duration}ms · ${if (success) "成功" else "失败"}")
                            appendLine()
                            appendLine("**参数：**")
                            appendLine("```json")
                            appendLine(args)
                            appendLine("```")
                            appendLine()
                            appendLine("**结果：**")
                            appendLine("```json")
                            appendLine(result)
                            appendLine("```")
                            appendLine()
                        }
                        appendLine("</details>")
                        appendLine()
                    }
                }
            }

            appendLine(msg.content)
            appendLine()
            appendLine("---")
            appendLine()
        }
    }

    fun exportJson(
        conversation: ConversationEntity,
        messages: List<ConversationMessageEntity>,
        options: ExportOptions,
    ): String {
        val obj = buildJsonObject {
            put("conversation", buildJsonObject {
                put("id", conversation.id)
                put("title", conversation.title)
                put("createdAt", conversation.createdAt)
                put("updatedAt", conversation.updatedAt)
            })
            put("messages", buildJsonArray {
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("id", msg.id)
                        put("order", msg.order)
                        put("role", msg.role)
                        put("content", msg.content)
                        put("createdAt", msg.createdAt)

                        if (options.includeReasoning && msg.reasoning.isNotBlank()) {
                            put("reasoning", msg.reasoning)
                        }
                        if (options.includeTools && msg.toolCallsJson.isNotBlank()) {
                            val calls = runCatching {
                                json.parseToJsonElement(msg.toolCallsJson).jsonArray
                            }.getOrNull()
                            if (calls != null) {
                                put("toolCalls", calls)
                            }
                        }
                    })
                }
            })
            put("exportedAt", System.currentTimeMillis())
            put("schemaVersion", 1)
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }
}
