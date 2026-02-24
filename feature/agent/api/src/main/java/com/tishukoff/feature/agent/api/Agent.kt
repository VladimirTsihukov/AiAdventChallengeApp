package com.tishukoff.feature.agent.api

import kotlinx.coroutines.flow.Flow

interface Agent {
    val conversationHistory: Flow<List<ChatMessage>>
    val settings: LlmSettings
    fun updateSettings(newSettings: LlmSettings)
    suspend fun loadHistory()
    suspend fun addUserMessage(text: String): ChatMessage
    suspend fun processRequest(): ChatMessage
    suspend fun clearHistory()
}
