package com.tishukoff.feature.agent.impl

import com.tishukoff.core.database.api.ChatMessageRecord
import com.tishukoff.core.database.api.ContextSummaryRecord
import com.tishukoff.core.database.api.ContextSummaryStorage
import com.tishukoff.feature.agent.api.ClaudeModel
import com.tishukoff.feature.agent.api.CompressionSettings
import com.tishukoff.feature.agent.api.CompressionStats
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal data class CompressedContext(
    val summaryPrefix: String,
    val recentMessages: List<ChatMessageRecord>,
    val stats: CompressionStats,
)

internal class ContextCompressor(
    private val apiKey: String,
    private val client: OkHttpClient,
    private val contextSummaryStorage: ContextSummaryStorage,
) {

    suspend fun buildCompressedContext(
        chatId: Long,
        allMessages: List<ChatMessageRecord>,
        settings: CompressionSettings,
    ): CompressedContext {
        val totalCount = allMessages.size
        val recentCount = settings.recentMessagesToKeep

        if (totalCount <= recentCount) {
            return CompressedContext(
                summaryPrefix = "",
                recentMessages = allMessages,
                stats = CompressionStats(isEnabled = true),
            )
        }

        val recentMessages = allMessages.takeLast(recentCount)
        val oldMessages = allMessages.dropLast(recentCount)
        val maxCoveredIndex = contextSummaryStorage.getMaxCoveredIndex(chatId) ?: -1
        val batchSize = settings.summarizationBatchSize

        val uncoveredMessages = if (maxCoveredIndex >= 0) {
            oldMessages.drop(maxCoveredIndex + 1)
        } else {
            oldMessages
        }

        val startIndex = maxCoveredIndex + 1
        val fullBatchCount = uncoveredMessages.size / batchSize

        for (i in 0 until fullBatchCount) {
            val batchStart = i * batchSize
            val batchEnd = batchStart + batchSize
            val batch = uncoveredMessages.subList(batchStart, batchEnd)
            val summaryText = generateSummary(batch)
            val originalEstimate = batch.sumOf { estimateTokens(it.text) }
            val summaryEstimate = estimateTokens(summaryText)

            contextSummaryStorage.insert(
                ContextSummaryRecord(
                    chatId = chatId,
                    summaryText = summaryText,
                    fromMessageIndex = startIndex + batchStart,
                    toMessageIndex = startIndex + batchEnd - 1,
                    originalTokenEstimate = originalEstimate,
                    summaryTokenEstimate = summaryEstimate,
                )
            )
        }

        val remainderStart = fullBatchCount * batchSize
        val remainderMessages = uncoveredMessages.drop(remainderStart)

        val existingSummaries = contextSummaryStorage.getSummariesByChatId(chatId)

        val summaryText = existingSummaries.joinToString("\n\n") { it.summaryText }
        val fullPrefix = buildString {
            if (summaryText.isNotBlank()) {
                append("[Conversation summary]\n")
                append(summaryText)
            }
            if (remainderMessages.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append("[Earlier messages not yet summarized]\n")
                for (msg in remainderMessages) {
                    val role = if (msg.isUser) "User" else "Assistant"
                    append("$role: ${msg.text}\n")
                }
            }
        }

        val messagesForApi = recentMessages

        val originalTokenEstimate = allMessages.sumOf { estimateTokens(it.text) }
        val compressedTokenEstimate = estimateTokens(fullPrefix) +
                messagesForApi.sumOf { estimateTokens(it.text) }
        val tokensSaved = (originalTokenEstimate - compressedTokenEstimate).coerceAtLeast(0)
        val compressionRatio = if (originalTokenEstimate > 0) {
            1f - compressedTokenEstimate.toFloat() / originalTokenEstimate.toFloat()
        } else {
            0f
        }

        return CompressedContext(
            summaryPrefix = fullPrefix,
            recentMessages = messagesForApi,
            stats = CompressionStats(
                isEnabled = true,
                summaryCount = existingSummaries.size,
                originalTokenEstimate = originalTokenEstimate,
                compressedTokenEstimate = compressedTokenEstimate,
                tokensSaved = tokensSaved,
                compressionRatio = compressionRatio,
            ),
        )
    }

    private fun generateSummary(messages: List<ChatMessageRecord>): String {
        val conversationText = messages.joinToString("\n") { msg ->
            val role = if (msg.isUser) "User" else "Assistant"
            "$role: ${msg.text}"
        }

        val prompt = """Summarize the following conversation concisely, preserving key facts, decisions, and context needed for continuation. Keep it under 200 words.

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

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return "[Summary generation failed]"

        return if (response.isSuccessful) {
            val json = JSONObject(body)
            val content = json.getJSONArray("content")
            if (content.length() > 0) {
                content.getJSONObject(0).getString("text")
            } else {
                "[Empty summary]"
            }
        } else {
            "[Summary generation failed: ${response.code}]"
        }
    }

    companion object {
        fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)
    }
}
