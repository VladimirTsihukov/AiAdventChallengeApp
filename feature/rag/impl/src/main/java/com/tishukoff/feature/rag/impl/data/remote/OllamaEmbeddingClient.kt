package com.tishukoff.feature.rag.impl.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

internal class OllamaEmbeddingClient(
    private val baseUrl: String = "http://192.168.100.3:11434",
    private val model: String = "nomic-embed-text",
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getEmbedding(text: String): FloatArray = withContext(Dispatchers.IO) {
        val requestBody = buildJsonObject {
            put("model", model)
            put("prompt", text)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/embeddings")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Ollama API error: ${response.code} ${response.body?.string()}")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Empty response from Ollama")

        val jsonResponse = json.decodeFromString<JsonObject>(body)
        val embeddingArray = jsonResponse["embedding"]?.jsonArray
            ?: throw RuntimeException("No embedding in response")

        FloatArray(embeddingArray.size) { i ->
            embeddingArray[i].jsonPrimitive.float
        }
    }
}
