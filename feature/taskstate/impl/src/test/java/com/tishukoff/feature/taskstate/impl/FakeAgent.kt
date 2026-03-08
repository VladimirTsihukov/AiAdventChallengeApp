package com.tishukoff.feature.taskstate.impl

import com.tishukoff.feature.agent.api.Agent
import com.tishukoff.feature.agent.api.BranchInfo
import com.tishukoff.feature.agent.api.ChatMessage
import com.tishukoff.feature.agent.api.CompressionStats
import com.tishukoff.feature.agent.api.LlmSettings
import com.tishukoff.feature.agent.api.TokenStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAgent : Agent {

    private val _settings = MutableStateFlow(LlmSettings())
    override val settings: Flow<LlmSettings> = _settings

    override val conversationHistory: Flow<List<ChatMessage>> = MutableStateFlow(emptyList())
    override val currentChatId: Flow<Long?> = MutableStateFlow(null)
    override val tokenStats: Flow<TokenStats> = MutableStateFlow(TokenStats())
    override val compressionStats: Flow<CompressionStats> = MutableStateFlow(CompressionStats())
    override val branches: Flow<List<BranchInfo>> = MutableStateFlow(emptyList())
    override val currentBranchId: Flow<String?> = MutableStateFlow(null)

    var nextResponse: String = "fake response"

    override fun updateSettings(newSettings: LlmSettings) {
        _settings.value = newSettings
    }

    override suspend fun addUserMessage(text: String): ChatMessage =
        ChatMessage(text = text, isUser = true)

    override suspend fun processRequest(): ChatMessage =
        ChatMessage(text = nextResponse, isUser = false)

    override suspend fun clearHistory() = Unit
    override suspend fun selectChat(chatId: Long) = Unit
    override fun startNewChat() = Unit
    override fun createCheckpoint(name: String) = Unit
    override fun createBranch(checkpointId: String, name: String) = Unit
    override fun switchBranch(branchId: String) = Unit
}
