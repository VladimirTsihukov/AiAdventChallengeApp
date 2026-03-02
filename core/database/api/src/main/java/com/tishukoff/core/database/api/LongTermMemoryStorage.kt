package com.tishukoff.core.database.api

import kotlinx.coroutines.flow.Flow

interface LongTermMemoryStorage {
    fun getAll(): Flow<List<LongTermMemoryRecord>>
    suspend fun getAllOnce(): List<LongTermMemoryRecord>
    fun getByCategory(category: String): Flow<List<LongTermMemoryRecord>>
    suspend fun upsert(record: LongTermMemoryRecord)
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
}
