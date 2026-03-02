package com.tishukoff.core.database.impl

import com.tishukoff.core.database.api.LongTermMemoryRecord
import com.tishukoff.core.database.api.LongTermMemoryStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomLongTermMemoryStorage(
    private val dao: LongTermMemoryDao,
) : LongTermMemoryStorage {

    override fun getAll(): Flow<List<LongTermMemoryRecord>> =
        dao.getAll().map { entities ->
            entities.map { it.toRecord() }
        }

    override suspend fun getAllOnce(): List<LongTermMemoryRecord> =
        dao.getAllOnce().map { it.toRecord() }

    override fun getByCategory(category: String): Flow<List<LongTermMemoryRecord>> =
        dao.getByCategory(category).map { entities ->
            entities.map { it.toRecord() }
        }

    override suspend fun upsert(record: LongTermMemoryRecord) {
        dao.upsert(record.toEntity())
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}

private fun LongTermMemoryEntity.toRecord() = LongTermMemoryRecord(
    id = id,
    key = key,
    value = value,
    category = category,
    timestamp = timestamp,
)

private fun LongTermMemoryRecord.toEntity() = LongTermMemoryEntity(
    id = id,
    key = key,
    value = value,
    category = category,
    timestamp = timestamp,
)
