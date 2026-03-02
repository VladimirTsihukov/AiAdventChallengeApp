package com.tishukoff.feature.agent.impl.strategy

import com.tishukoff.core.database.api.ChatMessageRecord
import com.tishukoff.feature.agent.api.CompressionStats
import com.tishukoff.feature.agent.api.LlmSettings

internal interface ContextStrategy {
    suspend fun buildContext(
        chatId: Long,
        allMessages: List<ChatMessageRecord>,
        settings: LlmSettings,
    ): StrategyContext
}

internal data class StrategyContext(
    val systemPromptPrefix: String,
    val messagesToSend: List<ChatMessageRecord>,
    val stats: CompressionStats,
)
