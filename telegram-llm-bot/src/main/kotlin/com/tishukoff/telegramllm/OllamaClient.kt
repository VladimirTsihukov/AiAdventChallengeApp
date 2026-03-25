package com.tishukoff.telegramllm

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * HTTP-клиент для Ollama /api/chat endpoint.
 */
class OllamaClient(
    private val baseUrl: String = "http://192.168.100.5:11434",
    private val model: String = "phi3:mini",
) {

    private val httpClient = HttpClient(CIO) {
        engine {
            endpoint {
                connectTimeout = 30_000
                requestTimeout = 0
                socketTimeout = 120_000
            }
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Отправляет историю сообщений в Ollama и возвращает ответ модели.
     *
     * @param messages история в формате (role, content)
     * @return текст ответа от модели
     */
    suspend fun chat(messages: List<Pair<String, String>>): String {
        val requestBody = buildJsonObject {
            put("model", model)
            put("stream", false)
            put("messages", buildJsonArray {
                messages.forEach { (role, content) ->
                    add(buildJsonObject {
                        put("role", role)
                        put("content", content)
                    })
                }
            })
        }

        val response = httpClient.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        if (!response.status.isSuccess()) {
            val error = response.bodyAsText()
            throw RuntimeException("Ollama API error ${response.status.value}: $error")
        }

        val body = response.bodyAsText()
        val jsonResponse = json.decodeFromString<JsonObject>(body)

        return jsonResponse["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw RuntimeException("No message content in Ollama response")
    }
}
