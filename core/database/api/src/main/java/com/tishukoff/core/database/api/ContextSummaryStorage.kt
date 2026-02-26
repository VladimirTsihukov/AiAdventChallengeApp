package com.tishukoff.core.database.api

interface ContextSummaryStorage {

    suspend fun getSummariesByChatId(chatId: Long): List<ContextSummaryRecord>

    suspend fun insert(record: ContextSummaryRecord)

    suspend fun deleteByChatId(chatId: Long)

    suspend fun getMaxCoveredIndex(chatId: Long): Int?
}
