package com.tishukoff.feature.agent.impl.strategy

import com.tishukoff.core.database.api.ChatMessageRecord
import com.tishukoff.feature.agent.api.CompressionStats
import com.tishukoff.feature.agent.api.LlmSettings
import com.tishukoff.feature.agent.impl.ContextCompressor

internal class SummarizationStrategy(
    private val contextCompressor: ContextCompressor,
) : ContextStrategy {

    override suspend fun buildContext(
        chatId: Long,
        allMessages: List<ChatMessageRecord>,
        settings: LlmSettings,
    ): StrategyContext {
        if (!settings.compression.enabled) {
            return StrategyContext(
                systemPromptPrefix = "",
                messagesToSend = allMessages,
                stats = CompressionStats(),
            )
        }

        val compressed = contextCompressor.buildCompressedContext(
            chatId = chatId,
            allMessages = allMessages,
            settings = settings.compression,
        )

        return StrategyContext(
            systemPromptPrefix = compressed.summaryPrefix,
            messagesToSend = compressed.recentMessages,
            stats = compressed.stats,
        )
    }
}
