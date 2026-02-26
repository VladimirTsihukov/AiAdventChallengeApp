package com.tishukoff.feature.agent.api

import kotlinx.coroutines.flow.Flow

interface Agent {
    val conversationHistory: Flow<List<ChatMessage>>
    val currentChatId: Flow<Long?>
    val settings: LlmSettings

    /** Accumulated token statistics for the current dialog. */
    val tokenStats: Flow<TokenStats>

    fun updateSettings(newSettings: LlmSettings)
    suspend fun addUserMessage(text: String): ChatMessage
    suspend fun processRequest(): ChatMessage
    suspend fun clearHistory()

    /** Switches the agent to work with the specified chat. */
    suspend fun selectChat(chatId: Long)

    /** Creates a new empty chat (not persisted until first message). */
    fun startNewChat()
}
