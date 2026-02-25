package com.tishukoff.core.database.api

/**
 * Storage for persisting chat message history across app restarts.
 */
interface ChatHistoryStorage {

    /** Returns all saved messages ordered by creation time. */
    suspend fun getAll(): List<ChatMessageRecord>

    /** Saves a single message to persistent storage. */
    suspend fun insert(record: ChatMessageRecord)

    /** Removes all saved messages. */
    suspend fun deleteAll()
}
