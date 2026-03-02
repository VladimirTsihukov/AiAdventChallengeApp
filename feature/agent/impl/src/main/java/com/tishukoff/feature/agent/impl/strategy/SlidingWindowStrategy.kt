package com.tishukoff.feature.agent.impl.strategy

import com.tishukoff.core.database.api.ChatMessageRecord
import com.tishukoff.feature.agent.api.CompressionStats
import com.tishukoff.feature.agent.api.LlmSettings
import com.tishukoff.feature.agent.impl.ContextCompressor

internal class SlidingWindowStrategy : ContextStrategy {

    override suspend fun buildContext(
        chatId: Long,
        allMessages: List<ChatMessageRecord>,
        settings: LlmSettings,
    ): StrategyContext {
        val windowSize = settings.compression.recentMessagesToKeep

        if (allMessages.size <= windowSize) {
            return StrategyContext(
                systemPromptPrefix = "",
                messagesToSend = allMessages,
                stats = CompressionStats(isEnabled = true),
            )
        }

        val messagesToSend = allMessages.takeLast(windowSize)
        val droppedMessages = allMessages.dropLast(windowSize)

        val originalTokenEstimate = allMessages.sumOf { ContextCompressor.estimateTokens(it.text) }
        val keptTokenEstimate = messagesToSend.sumOf { ContextCompressor.estimateTokens(it.text) }
        val tokensSaved = (originalTokenEstimate - keptTokenEstimate).coerceAtLeast(0)
        val compressionRatio = if (originalTokenEstimate > 0) {
            1f - keptTokenEstimate.toFloat() / originalTokenEstimate.toFloat()
        } else {
            0f
        }

        return StrategyContext(
            systemPromptPrefix = "",
            messagesToSend = messagesToSend,
            stats = CompressionStats(
                isEnabled = true,
                summaryCount = droppedMessages.size,
                originalTokenEstimate = originalTokenEstimate,
                compressedTokenEstimate = keptTokenEstimate,
                tokensSaved = tokensSaved,
                compressionRatio = compressionRatio,
            ),
        )
    }
}
