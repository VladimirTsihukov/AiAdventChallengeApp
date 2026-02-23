package com.tishukoff.feature.agent.api

interface Agent {
    val conversationHistory: List<ChatMessage>
    val settings: LlmSettings
    fun updateSettings(newSettings: LlmSettings)
    fun addUserMessage(text: String): ChatMessage
    suspend fun processRequest(): ChatMessage
    fun clearHistory()
}
