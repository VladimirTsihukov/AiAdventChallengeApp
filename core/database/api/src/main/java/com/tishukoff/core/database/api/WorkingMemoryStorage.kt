package com.tishukoff.core.database.api

import kotlinx.coroutines.flow.Flow

interface WorkingMemoryStorage {
    fun getByChatId(chatId: Long): Flow<List<WorkingMemoryRecord>>
    suspend fun getByChatIdOnce(chatId: Long): List<WorkingMemoryRecord>
    suspend fun upsert(record: WorkingMemoryRecord)
    suspend fun deleteByChatId(chatId: Long)
    suspend fun deleteByKey(chatId: Long, key: String)
}
