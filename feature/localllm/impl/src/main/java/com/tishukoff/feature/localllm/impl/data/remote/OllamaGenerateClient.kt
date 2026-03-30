package com.tishukoff.feature.localllm.impl.data.remote

import com.tishukoff.feature.localllm.impl.domain.model.LlmConfig
import com.tishukoff.feature.localllm.impl.domain.model.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Клиент для генерации текста через Ollama /api/chat endpoint.
 */
internal class OllamaGenerateClient(
    baseUrl: String = DEFAULT_BASE_URL,
) {

    private var _baseUrl: String = baseUrl
    val currentBaseUrl: String get() = _baseUrl
    private val baseUrlMutex = Mutex()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val healthClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val concurrencySemaphore = Semaphore(MAX_CONCURRENT_REQUESTS)
    private val requestTimestamps = ConcurrentLinkedQueue<Long>()

    /**
     * Обновляет базовый URL сервера.
     */
    suspend fun updateBaseUrl(newBaseUrl: String) {
        baseUrlMutex.lock()
        try {
            _baseUrl = newBaseUrl
        } finally {
            baseUrlMutex.unlock()
        }
    }

    /**
     * Проверяет, не превышен ли rate limit.
     *
     * @return true если запрос разрешён
     */
    private fun checkRateLimit(): Boolean {
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000
        while (requestTimestamps.peek()?.let { it < oneMinuteAgo } == true) {
            requestTimestamps.poll()
        }
        return requestTimestamps.size < MAX_REQUESTS_PER_MINUTE
    }

    /**
     * Возвращает количество запросов за последнюю минуту.
     */
    fun getRequestsInLastMinute(): Int {
        val oneMinuteAgo = System.currentTimeMillis() - 60_000
        while (requestTimestamps.peek()?.let { it < oneMinuteAgo } == true) {
            requestTimestamps.poll()
        }
        return requestTimestamps.size
    }

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
        if (!checkRateLimit()) {
            throw RuntimeException(
                "Rate limit exceeded: max $MAX_REQUESTS_PER_MINUTE requests per minute"
            )
        }

        concurrencySemaphore.acquire()
        try {
            requestTimestamps.add(System.currentTimeMillis())

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
                .url("$_baseUrl/api/chat")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw RuntimeException(
                    "Ollama API error: ${response.code} ${response.body?.string()}"
                )
            }

            val body = response.body?.string()
                ?: throw RuntimeException("Empty response from Ollama")

            val jsonResponse = json.decodeFromString<JsonObject>(body)
            jsonResponse["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: throw RuntimeException("No message content in response")
        } finally {
            concurrencySemaphore.release()
        }
    }

    /**
     * Проверяет доступность сервера.
     *
     * @return время отклика в миллисекундах, или -1 если сервер недоступен
     */
    suspend fun ping(): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("$_baseUrl/api/tags")
                .get()
                .build()
            val response = healthClient.newCall(request).execute()
            response.close()
            if (response.isSuccessful) {
                System.currentTimeMillis() - start
            } else {
                -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Возвращает список доступных моделей.
     */
    suspend fun listModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$_baseUrl/api/tags")
            .get()
            .build()

        val response = healthClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Failed to list models: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Empty response from /api/tags")

        val jsonResponse = json.decodeFromString<JsonObject>(body)
        val models = jsonResponse["models"]?.jsonArray ?: return@withContext emptyList()

        models.map { element ->
            val obj = element.jsonObject
            ModelInfo(
                name = obj["name"]?.jsonPrimitive?.content ?: "unknown",
                size = obj["size"]?.jsonPrimitive?.long ?: 0L,
            )
        }
    }

    /**
     * Возвращает версию Ollama.
     */
    suspend fun getVersion(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$_baseUrl/api/version")
            .get()
            .build()

        val response = healthClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Failed to get version: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Empty response from /api/version")

        val jsonResponse = json.decodeFromString<JsonObject>(body)
        jsonResponse["version"]?.jsonPrimitive?.content ?: "unknown"
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://192.168.100.5:11434"
        const val MAX_REQUESTS_PER_MINUTE = 10
        const val MAX_CONCURRENT_REQUESTS = 2
    }
}
