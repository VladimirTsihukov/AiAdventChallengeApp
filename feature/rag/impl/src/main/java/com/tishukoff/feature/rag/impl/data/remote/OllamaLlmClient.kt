package com.tishukoff.feature.rag.impl.data.remote

import com.tishukoff.feature.rag.impl.domain.repository.LlmClient
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
 * Реализация [LlmClient] через Ollama /api/chat endpoint.
 */
class OllamaLlmClient(
    private val baseUrl: String = "http://192.168.100.5:11434",
    private val model: String = "phi3:mini",
) : LlmClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(prompt: String): String {
        return chat(
            messages = listOf("user" to prompt),
        )
    }

    override suspend fun chat(
        messages: List<Pair<String, String>>,
        systemPrompt: String,
    ): String {
        val allMessages = buildList {
            if (systemPrompt.isNotBlank()) {
                add("system" to systemPrompt)
            }
            addAll(messages)
        }

        val requestBody = buildJsonObject {
            put("model", model)
            put("stream", false)
            put("messages", buildJsonArray {
                allMessages.forEach { (role, content) ->
                    add(buildJsonObject {
                        put("role", role)
                        put("content", content)
                    })
                }
            })
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .post(requestBody)
            .build()

        val response = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            error("Ollama API error: ${response.code} ${response.body?.string()}")
        }

        val body = response.body?.string() ?: error("Empty response from Ollama")
        val jsonResponse = json.decodeFromString<JsonObject>(body)
        return jsonResponse["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: error("No message content in response")
    }
}
