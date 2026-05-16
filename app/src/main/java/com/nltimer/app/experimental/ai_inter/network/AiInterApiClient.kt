package com.nltimer.app.experimental.ai_inter.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiInterApiClient @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class ChatMessage(
        val role: String,
        val content: String
    )

    data class ChatResult(
        val content: String,
        val promptTokens: Int,
        val responseTokens: Int
    )

    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.isNotBlank()) { "API 地址未配置" }
            val url = "${baseUrl.trimEnd('/')}/models"
            val request = Request.Builder()
                .url(url)
                .apply { if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey") }
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val modelsResponse = json.decodeFromString<OpenAiModelsResponse>(body)
                modelsResponse.data.map { it.id }.sorted()
            } else {
                val errorMsg = parseErrorMessage(body, response.message)
                throw Exception("HTTP ${response.code}: $errorMsg")
            }
        }.recoverCatching { e ->
            throw Exception(describeError(e), e)
        }
    }

    suspend fun chat(
        baseUrl: String, path: String, apiKey: String,
        model: String, messages: List<ChatMessage>
    ): Result<ChatResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.isNotBlank()) { "API 地址未配置" }
            require(path.isNotBlank()) { "API 路径未配置" }
            require(model.isNotBlank()) { "模型名未配置" }
            val url = baseUrl.trimEnd('/') + path
            val requestBody = OpenAiChatRequest(
                model = model,
                messages = messages.map { OpenAiChatMessage(role = it.role, content = it.content) },
                stream = false
            )
            val jsonBody = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url(url)
                .apply { if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey") }
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val chatResponse = json.decodeFromString<OpenAiChatResponse>(body)
                val content = chatResponse.choices.firstOrNull()?.message?.content ?: ""
                ChatResult(
                    content = content,
                    promptTokens = chatResponse.usage?.promptTokens ?: 0,
                    responseTokens = chatResponse.usage?.completionTokens ?: 0
                )
            } else {
                val errorMsg = parseErrorMessage(body, response.message)
                throw Exception("HTTP ${response.code}: $errorMsg")
            }
        }.recoverCatching { e ->
            throw Exception(describeError(e), e)
        }
    }

    fun streamChat(
        baseUrl: String, path: String, apiKey: String,
        model: String, messages: List<ChatMessage>
    ): Flow<String> = callbackFlow {
        val url = baseUrl.trimEnd('/') + path
        val host = try { url.toHttpUrl().host } catch (_: Exception) { "" }

        val requestBody = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }
            put("stream", true)
            // 借鉴 rikkahub：mistral 不支持 stream_options，其他服务都受益
            if (host != "api.mistral.ai") {
                putJsonObject("stream_options") {
                    put("include_usage", true)
                }
            }
        }
        val jsonBody = json.encodeToString(JsonObject.serializer(), requestBody)

        val request = Request.Builder()
            .url(url)
            .apply { if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey") }
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .apply {
                // 借鉴 rikkahub 的 referer 头
                when (host) {
                    "openrouter.ai" -> {
                        addHeader("X-Title", "NLtimer")
                        addHeader("HTTP-Referer", "https://github.com/")
                    }
                }
            }
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        val factory = EventSources.createFactory(client)
        val eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data.trim() == "[DONE]") {
                    close()
                    return
                }
                // 借鉴 rikkahub：data 内可能是多行 JSON，逐行处理
                data.trim().split("\n").filter { it.isNotBlank() }.forEach { line ->
                    try {
                        val element = json.parseToJsonElement(line)
                        if (element !is JsonObject) return@forEach

                        // 错误帧
                        if (element["error"] != null) {
                            close(parseErrorDetail(element))
                            return
                        }

                        val choices = element["choices"]?.jsonArray ?: return@forEach
                        if (choices.isEmpty()) return@forEach
                        val delta = choices[0].jsonObject["delta"]?.jsonObject
                            ?: choices[0].jsonObject["message"]?.jsonObject
                            ?: return@forEach

                        val content = delta["content"]?.let {
                            (it as? JsonPrimitive)?.contentOrNull
                        }
                        val reasoning = delta["reasoning_content"]?.let {
                            (it as? JsonPrimitive)?.contentOrNull
                        } ?: delta["reasoning"]?.let {
                            (it as? JsonPrimitive)?.contentOrNull
                        }

                        if (!content.isNullOrEmpty()) {
                            trySend(content)
                        } else if (!reasoning.isNullOrEmpty()) {
                            // reasoning 模型尚未给出最终回答，但思考过程也发给 UI
                            trySend(reasoning)
                        }
                    } catch (_: Exception) {
                        // 跳过单行 parse 失败
                    }
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: okhttp3.Response?
            ) {
                val message = buildString {
                    if (response != null) {
                        append("HTTP ${response.code} @ ").append(response.request.url)
                        val bodyStr = try { response.body?.string() } catch (_: Exception) { null }
                        if (!bodyStr.isNullOrBlank()) {
                            val parsed = try {
                                val element = json.parseToJsonElement(bodyStr)
                                parseErrorDetail(element).message ?: bodyStr.take(500)
                            } catch (_: Exception) {
                                bodyStr.take(500)
                            }
                            append("\n").append(parsed)
                        } else if (response.message.isNotBlank()) {
                            append("\n").append(response.message)
                        }
                    }
                    if (t != null) {
                        if (isNotEmpty()) append("\n— ")
                        val cls = t::class.simpleName ?: "Exception"
                        val msg = t.message
                        append(if (msg.isNullOrBlank()) cls else "$cls: $msg")
                    }
                    if (isEmpty()) append("SSE connection failed @ ").append(url)
                }
                close(Exception(message, t))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose {
            eventSource.cancel()
        }
    }

    /**
     * 借鉴 rikkahub 的 parseErrorDetail：递归从 JsonObject 中找
     * error / detail / message / description 字段
     */
    private fun parseErrorDetail(element: JsonElement): Exception {
        return when (element) {
            is JsonObject -> {
                val errorFields = listOf("error", "detail", "message", "description")
                val foundField = errorFields.firstOrNull { element[it] != null }
                if (foundField != null) {
                    parseErrorDetail(element[foundField]!!)
                } else {
                    Exception(json.encodeToString(JsonElement.serializer(), element))
                }
            }
            is JsonArray -> {
                if (element.isEmpty()) Exception("Unknown error: empty array")
                else parseErrorDetail(element.first())
            }
            is JsonPrimitive -> Exception(element.content)
        }
    }

    private fun parseErrorMessage(body: String, fallback: String): String {
        return try {
            json.decodeFromString<OpenAiErrorResponse>(body).error?.message ?: fallback
        } catch (_: Exception) {
            body.takeIf { it.isNotBlank() && it.length < 500 } ?: fallback
        }
    }

    private fun describeError(e: Throwable): String {
        val msg = e.message?.takeIf { it.isNotBlank() }
        val cls = e::class.simpleName ?: "Exception"
        return when {
            msg != null -> "$cls: $msg"
            else -> "$cls（无错误描述，请检查 API 地址、Key 与网络）"
        }
    }
}
