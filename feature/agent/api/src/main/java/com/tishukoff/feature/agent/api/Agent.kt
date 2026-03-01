package com.tishukoff.feature.agent.api

import kotlinx.coroutines.flow.Flow

interface Agent {
    val conversationHistory: Flow<List<ChatMessage>>
    val currentChatId: Flow<Long?>
    val settings: Flow<LlmSettings>

    /** Accumulated token statistics for the current dialog. */
    val tokenStats: Flow<TokenStats>

    /** Live compression statistics for the current dialog. */
    val compressionStats: Flow<CompressionStats>

    /** Available dialog branches (only meaningful for BRANCHING strategy). */
    val branches: Flow<List<BranchInfo>>

    /** Currently active branch ID. */
    val currentBranchId: Flow<String?>

    fun updateSettings(newSettings: LlmSettings)
    suspend fun addUserMessage(text: String): ChatMessage
    suspend fun processRequest(): ChatMessage
    suspend fun clearHistory()

    /** Switches the agent to work with the specified chat. */
    suspend fun selectChat(chatId: Long)

    /** Creates a new empty chat (not persisted until first message). */
    fun startNewChat()

    /** Creates a checkpoint at the current point in conversation. */
    fun createCheckpoint(name: String)

    /** Creates a new branch from a checkpoint. */
    fun createBranch(checkpointId: String, name: String)

    /** Switches to a different dialog branch. */
    fun switchBranch(branchId: String)
}
