package com.tishukoff.aiadventchallengeapp.presentation.ui.models

import com.tishukoff.core.database.api.ChatRecord
import com.tishukoff.feature.agent.api.BranchInfo
import com.tishukoff.feature.agent.api.ChatMessage
import com.tishukoff.feature.agent.api.CompressionStats
import com.tishukoff.feature.agent.api.ContextStrategyType
import com.tishukoff.feature.agent.api.TokenStats

data class ChatUiState(
    val input: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val chats: List<ChatRecord> = emptyList(),
    val currentChatId: Long? = null,
    val tokenStats: TokenStats = TokenStats(),
    val compressionStats: CompressionStats = CompressionStats(),
    val branches: List<BranchInfo> = emptyList(),
    val currentBranchId: String? = null,
    val contextStrategyType: ContextStrategyType = ContextStrategyType.SUMMARIZATION,
)
