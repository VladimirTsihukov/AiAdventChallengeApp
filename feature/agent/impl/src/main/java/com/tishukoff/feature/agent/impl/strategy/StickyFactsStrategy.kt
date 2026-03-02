package com.tishukoff.feature.agent.impl.strategy

import com.tishukoff.core.database.api.ChatMessageRecord
import com.tishukoff.feature.agent.api.ClaudeModel
import com.tishukoff.feature.agent.api.CompressionStats
import com.tishukoff.feature.agent.api.LlmSettings
import com.tishukoff.feature.agent.impl.ContextCompressor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal class StickyFactsStrategy(
    private val apiKey: String,
    private val client: OkHttpClient,
) : ContextStrategy {

    private val factsStore = mutableMapOf<Long, MutableMap<String, String>>()
    private val processedMessageCount = mutableMapOf<Long, Int>()

    override suspend fun buildContext(
        chatId: Long,
        allMessages: List<ChatMessageRecord>,
        settings: LlmSettings,
    ): StrategyContext {
        val windowSize = settings.compression.recentMessagesToKeep
        val facts = factsStore.getOrPut(chatId) { mutableMapOf() }
        val alreadyProcessed = processedMessageCount[chatId] ?: 0

        if (allMessages.size > alreadyProcessed && alreadyProcessed > 0) {
            val newMessages = allMessages.subList(alreadyProcessed, allMessages.size)
            val extractedFacts = extractFacts(newMessages)
            facts.putAll(extractedFacts)
        } else if (alreadyProcessed == 0 && allMessages.size > windowSize) {
            val oldMessages = allMessages.dropLast(windowSize)
            val extractedFacts = extractFacts(oldMessages)
            facts.putAll(extractedFacts)
        }
        processedMessageCount[chatId] = allMessages.size

        val messagesToSend = if (allMessages.size <= windowSize) {
            allMessages
        } else {
            allMessages.takeLast(windowSize)
        }

        val factsPrefix = if (facts.isNotEmpty()) {
            buildString {
                append("[Known Facts]\n")
                facts.forEach { (key, value) ->
                    append("- $key: $value\n")
                }
            }
        } else {
            ""
        }

        val originalTokenEstimate = allMessages.sumOf { ContextCompressor.estimateTokens(it.text) }
        val keptTokenEstimate = messagesToSend.sumOf { ContextCompressor.estimateTokens(it.text) } +
            ContextCompressor.estimateTokens(factsPrefix)
        val tokensSaved = (originalTokenEstimate - keptTokenEstimate).coerceAtLeast(0)
        val compressionRatio = if (originalTokenEstimate > 0) {
            1f - keptTokenEstimate.toFloat() / originalTokenEstimate.toFloat()
        } else {
            0f
        }

        return StrategyContext(
            systemPromptPrefix = factsPrefix,
            messagesToSend = messagesToSend,
            stats = CompressionStats(
                isEnabled = true,
                summaryCount = facts.size,
                originalTokenEstimate = originalTokenEstimate,
                compressedTokenEstimate = keptTokenEstimate,
                tokensSaved = tokensSaved,
                compressionRatio = compressionRatio,
            ),
        )
    }

    private fun extractFacts(messages: List<ChatMessageRecord>): Map<String, String> {
        if (messages.isEmpty()) return emptyMap()

        val conversationText = messages.joinToString("\n") { msg ->
            val role = if (msg.isUser) "User" else "Assistant"
            "$role: ${msg.text}"
        }

        val prompt = """Extract key facts from this conversation as key-value pairs. Focus on:
- goal: the user's main objective
- constraints: any limitations or requirements mentioned
- preferences: user's stated preferences
- decisions: any decisions made
- agreements: things agreed upon

Return ONLY key-value pairs in format "key: value", one per line. No extra text.

Conversation:
$conversationText"""

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", ClaudeModel.HAIKU.apiId)
            put("max_tokens", 300)
            put("temperature", 0.0)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyMap()

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val content = json.getJSONArray("content")
                if (content.length() > 0) {
                    val text = content.getJSONObject(0).getString("text")
                    parseFacts(text)
                } else {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parseFacts(text: String): Map<String, String> {
        return text.lines()
            .filter { it.contains(":") }
            .associate { line ->
                val separatorIndex = line.indexOf(':')
                val key = line.substring(0, separatorIndex).trim().removePrefix("- ")
                val value = line.substring(separatorIndex + 1).trim()
                key to value
            }
    }
}
