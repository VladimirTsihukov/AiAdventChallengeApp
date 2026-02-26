package com.tishukoff.core.database.impl

import com.tishukoff.core.database.api.ChatRecord
import com.tishukoff.core.database.api.ChatStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomChatStorage(
    private val chatDao: ChatDao,
    private val messageDao: ChatMessageDao,
) : ChatStorage {

    override fun getAllChats(): Flow<List<ChatRecord>> {
        return chatDao.getAll().map { entities ->
            entities.map { it.toRecord() }
        }
    }

    override suspend fun createChat(title: String): Long {
        return chatDao.insert(
            ChatEntity(
                title = title,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun deleteChat(chatId: Long) {
        messageDao.deleteByChatId(chatId)
        chatDao.delete(chatId)
    }

    override suspend fun updateChatTitle(chatId: Long, title: String) {
        chatDao.updateTitle(chatId, title)
    }
}

private fun ChatEntity.toRecord() = ChatRecord(
    id = id,
    title = title,
    createdAt = createdAt,
)
