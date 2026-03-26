package com.tishukoff.feature.localllm.impl.data.remote

import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Клиент для генерации текста через Ollama /api/chat endpoint.
 */
internal class OllamaGenerateClient(
    private val baseUrl: String = "http://192.168.100.5:11434",
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Отправляет сообщение в Ollama и возвращает ответ модели.
     *
     * @param messages история сообщений в формате (role, content)
     * @param config конфигурация параметров LLM
     * @return текст ответа от модели
     */
    suspend fun chat(
        messages: List<Pair<String, String>>,
        config: LlmConfig = LlmConfig.DEFAULT,
    ): String = withContext(Dispatchers.IO) {
        val allMessages = buildList {
            if (config.systemPrompt.isNotBlank()) {
                add("system" to config.systemPrompt)
            }
            addAll(messages)
        }

        val requestBody = buildJsonObject {
            put("model", config.model)
            put("stream", false)
            put("messages", buildJsonArray {
                allMessages.forEach { (role, content) ->
                    add(buildJsonObject {
                        put("role", role)
                        put("content", content)
                    })
                }
            })
            put("options", buildJsonObject {
                put("temperature", config.temperature)
                put("num_predict", config.maxTokens)
                put("num_ctx", config.contextWindow)
                put("repeat_penalty", config.repeatPenalty)
                put("top_p", config.topP)
                put("top_k", config.topK)
            })
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Ollama API error: ${response.code} ${response.body?.string()}")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Empty response from Ollama")

        val jsonResponse = json.decodeFromString<JsonObject>(body)
        jsonResponse["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw RuntimeException("No message content in response")
    }
}
