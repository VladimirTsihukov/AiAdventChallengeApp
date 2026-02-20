package com.tishukoff.aiadventchallengeapp.data

import com.tishukoff.aiadventchallengeapp.BuildConfig
import com.tishukoff.aiadventchallengeapp.presentation.ui.models.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ClaudeRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        conversation: List<ChatMessage>,
        settings: LlmSettings
    ): Pair<String, ResponseMetadata> {
        return withContext(Dispatchers.IO) {
            val model = settings.model
            val messagesArray = JSONArray().apply {
                for (msg in conversation) {
                    put(JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "assistant")
                        put("content", msg.text)
                    })
                }
            }

            val jsonBody = JSONObject().apply {
                put("model", model.apiId)
                put("max_tokens", settings.maxTokens)
                put("temperature", settings.temperature.toDouble())
                if (settings.systemPrompt.isNotBlank()) {
                    put("system", settings.systemPrompt)
                }
                put("messages", messagesArray)
                if (settings.stopSequences.isNotEmpty()) {
                    put("stop_sequences", JSONArray(settings.stopSequences))
                }
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val startTime = System.currentTimeMillis()
            val response = client.newCall(request).execute()
            val elapsed = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: "Empty response"

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val content = json.getJSONArray("content")
                val text = if (content.length() > 0) {
                    content.getJSONObject(0).getString("text")
                } else {
                    "Empty response from Claude"
                }

                val usage = json.getJSONObject("usage")
                val inputTokens = usage.getInt("input_tokens")
                val outputTokens = usage.getInt("output_tokens")
                val cost = inputTokens * model.inputPricePerMillion / 1_000_000 +
                        outputTokens * model.outputPricePerMillion / 1_000_000

                val metadata = ResponseMetadata(
                    modelId = model.apiId,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    responseTimeMs = elapsed,
                    costUsd = cost
                )

                Pair(text, metadata)
            } else {
                error("Error ${response.code}: $body")
            }
        }
    }
}
