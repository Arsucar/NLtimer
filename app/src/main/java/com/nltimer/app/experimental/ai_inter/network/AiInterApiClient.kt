package com.nltimer.app.experimental.ai_inter.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
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

    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> {
        return try {
            val url = "${baseUrl.trimEnd('/')}/models"
            val request = Request.Builder()
                .url(url)
                .apply { if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey") }
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val modelsResponse = json.decodeFromString<OpenAiModelsResponse>(body)
                Result.success(modelsResponse.data.map { it.id }.sorted())
            } else {
                val errorMsg = parseErrorMessage(body, response.message)
                Result.failure(Exception("HTTP ${response.code}: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chat(
        baseUrl: String, path: String, apiKey: String,
        model: String, messages: List<ChatMessage>
    ): Result<ChatResult> {
        return try {
            val url = baseUrl.trimEnd('/') + path
            val requestBody = OpenAiChatRequest(
                model = model,
                messages = messages.map { OpenAiChatMessage(role = it.role, content = it.content) },
                stream = false
            )
            val jsonBody = json.encodeToString(OpenAiChatChatRequestSerializer, requestBody)
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
                Result.success(ChatResult(
                    content = content,
                    promptTokens = chatResponse.usage?.promptTokens ?: 0,
                    responseTokens = chatResponse.usage?.completionTokens ?: 0
                ))
            } else {
                val errorMsg = parseErrorMessage(body, response.message)
                Result.failure(Exception("HTTP ${response.code}: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChat(
        baseUrl: String, path: String, apiKey: String,
        model: String, messages: List<ChatMessage>
    ): Flow<String> = callbackFlow {
        val url = baseUrl.trimEnd('/') + path
        val requestBody = OpenAiChatRequest(
            model = model,
            messages = messages.map { OpenAiChatMessage(role = it.role, content = it.content) },
            stream = true
        )
        val jsonBody = json.encodeToString(OpenAiChatChatRequestSerializer, requestBody)
        val request = Request.Builder()
            .url(url)
            .apply { if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey") }
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        val factory = EventSources.createFactory(client)
        val eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                try {
                    val chunk = json.decodeFromString<OpenAiStreamChunk>(data)
                    val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                    if (content.isNotEmpty()) {
                        trySend(content)
                    }
                } catch (_: Exception) { }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                close(t ?: Exception("SSE connection failed"))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose { eventSource.cancel() }
    }

    private fun parseErrorMessage(body: String, fallback: String): String {
        return try {
            json.decodeFromString<OpenAiErrorResponse>(body).error?.message ?: fallback
        } catch (_: Exception) { fallback }
    }
}

private val OpenAiChatChatRequestSerializer = kotlinx.serialization.serializer<OpenAiChatRequest>()
