package com.tishukoff.feature.memory.api

import kotlinx.coroutines.flow.Flow

interface MemoryManager {

    suspend fun processNewMessage(chatId: Long, userMessage: String, assistantResponse: String)

    fun getWorkingMemory(chatId: Long): Flow<List<MemoryEntry>>

    fun getLongTermMemory(): Flow<List<MemoryEntry>>

    suspend fun buildMemoryPromptPrefix(chatId: Long): String

    suspend fun deleteWorkingMemoryEntry(chatId: Long, key: String)

    suspend fun deleteLongTermMemoryEntry(id: Long)
}
