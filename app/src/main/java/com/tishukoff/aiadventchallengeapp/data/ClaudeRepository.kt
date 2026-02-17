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

    suspend fun sendMessage(conversation: List<ChatMessage>, settings: LlmSettings): String {
        return withContext(Dispatchers.IO) {
            val messagesArray = JSONArray().apply {
                for (msg in conversation) {
                    put(JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "assistant")
                        put("content", msg.text)
                    })
                }
            }

            val jsonBody = JSONObject().apply {
                put("model", "claude-sonnet-4-5-20250929")
                put("max_tokens", settings.maxTokens)
                put("temperature", settings.temperature.toDouble())
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

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "Empty response"

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val content = json.getJSONArray("content")
                if (content.length() > 0) {
                    content.getJSONObject(0).getString("text")
                } else {
                    "Empty response from Claude"
                }
            } else {
                error("Error ${response.code}: $body")
            }
        }
    }
}
