package com.tishukoff.core.database.impl

import com.tishukoff.core.database.api.ChatHistoryStorage
import com.tishukoff.core.database.api.ChatMessageRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomChatHistoryStorage(
    private val dao: ChatMessageDao
) : ChatHistoryStorage {

    override fun getAll(): Flow<List<ChatMessageRecord>> {
        return dao.getAll().map { entities ->
            entities.map { it.toRecord() }
        }
    }

    override suspend fun getAllOnce(): List<ChatMessageRecord> {
        return dao.getAllOnce().map { it.toRecord() }
    }

    override suspend fun insert(record: ChatMessageRecord) {
        dao.insert(
            ChatMessageEntity(
                chatId = 0,
                text = record.text,
                isUser = record.isUser,
                metadataText = record.metadataText,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    override fun getByChatId(chatId: Long): Flow<List<ChatMessageRecord>> {
        return dao.getByChatId(chatId).map { entities ->
            entities.map { it.toRecord() }
        }
    }

    override suspend fun getByChatIdOnce(chatId: Long): List<ChatMessageRecord> {
        return dao.getByChatIdOnce(chatId).map { it.toRecord() }
    }

    override suspend fun insert(chatId: Long, record: ChatMessageRecord) {
        dao.insert(
            ChatMessageEntity(
                chatId = chatId,
                text = record.text,
                isUser = record.isUser,
                metadataText = record.metadataText,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun deleteByChatId(chatId: Long) {
        dao.deleteByChatId(chatId)
    }
}

private fun ChatMessageEntity.toRecord() = ChatMessageRecord(
    text = text,
    isUser = isUser,
    metadataText = metadataText,
)
