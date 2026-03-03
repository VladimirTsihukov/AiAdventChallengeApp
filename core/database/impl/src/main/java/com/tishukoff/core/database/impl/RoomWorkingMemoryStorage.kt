package com.tishukoff.core.database.impl

import com.tishukoff.core.database.api.WorkingMemoryRecord
import com.tishukoff.core.database.api.WorkingMemoryStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomWorkingMemoryStorage(
    private val dao: WorkingMemoryDao,
) : WorkingMemoryStorage {

    override fun getByChatId(chatId: Long): Flow<List<WorkingMemoryRecord>> =
        dao.getByChatId(chatId).map { entities ->
            entities.map { it.toRecord() }
        }

    override suspend fun getByChatIdOnce(chatId: Long): List<WorkingMemoryRecord> =
        dao.getByChatIdOnce(chatId).map { it.toRecord() }

    override suspend fun upsert(record: WorkingMemoryRecord) {
        dao.upsert(record.toEntity())
    }

    override suspend fun deleteByChatId(chatId: Long) {
        dao.deleteByChatId(chatId)
    }

    override suspend fun deleteByKey(chatId: Long, key: String) {
        dao.deleteByKey(chatId, key)
    }
}

private fun WorkingMemoryEntity.toRecord() = WorkingMemoryRecord(
    id = id,
    chatId = chatId,
    key = key,
    value = value,
    timestamp = timestamp,
)

private fun WorkingMemoryRecord.toEntity() = WorkingMemoryEntity(
    id = id,
    chatId = chatId,
    key = key,
    value = value,
    timestamp = timestamp,
)
