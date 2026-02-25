package com.tishukoff.core.database.api

import kotlinx.coroutines.flow.Flow

/**
 * Storage for persisting chat message history across app restarts.
 */
interface ChatHistoryStorage {

    /** Returns all saved messages ordered by creation time as a reactive stream. */
    fun getAll(): Flow<List<ChatMessageRecord>>

    /** Returns all saved messages as a snapshot (for building API requests). */
    suspend fun getAllOnce(): List<ChatMessageRecord>

    /** Saves a single message to persistent storage. */
    suspend fun insert(record: ChatMessageRecord)

    /** Removes all saved messages. */
    suspend fun deleteAll()

    /** Returns messages for a specific chat as a reactive stream. */
    fun getByChatId(chatId: Long): Flow<List<ChatMessageRecord>>

    /** Returns messages for a specific chat as a snapshot. */
    suspend fun getByChatIdOnce(chatId: Long): List<ChatMessageRecord>

    /** Saves a message to a specific chat. */
    suspend fun insert(chatId: Long, record: ChatMessageRecord)

    /** Removes all messages for a specific chat. */
    suspend fun deleteByChatId(chatId: Long)
}
