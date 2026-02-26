package com.tishukoff.core.database.api

import kotlinx.coroutines.flow.Flow

/**
 * Storage for managing chat conversations.
 */
interface ChatStorage {

    /** Returns all chats ordered by creation time (newest first) as a reactive stream. */
    fun getAllChats(): Flow<List<ChatRecord>>

    /** Creates a new chat and returns its id. */
    suspend fun createChat(title: String): Long

    /** Deletes a chat and all its messages. */
    suspend fun deleteChat(chatId: Long)

    /** Updates the title of an existing chat. */
    suspend fun updateChatTitle(chatId: Long, title: String)
}
