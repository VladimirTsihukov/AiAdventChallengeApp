package com.tishukoff.core.database.impl

import com.tishukoff.core.database.api.ChatHistoryStorage
import com.tishukoff.core.database.api.ChatMessageRecord

internal class RoomChatHistoryStorage(
    private val dao: ChatMessageDao
) : ChatHistoryStorage {

    override suspend fun getAll(): List<ChatMessageRecord> {
        return dao.getAll().map { entity ->
            ChatMessageRecord(
                text = entity.text,
                isUser = entity.isUser,
                metadataText = entity.metadataText
            )
        }
    }

    override suspend fun insert(record: ChatMessageRecord) {
        dao.insert(
            ChatMessageEntity(
                text = record.text,
                isUser = record.isUser,
                metadataText = record.metadataText,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}
