package com.tishukoff.core.database.impl

import com.tishukoff.core.database.api.ContextSummaryRecord
import com.tishukoff.core.database.api.ContextSummaryStorage

internal class RoomContextSummaryStorage(
    private val dao: ContextSummaryDao,
) : ContextSummaryStorage {

    override suspend fun getSummariesByChatId(chatId: Long): List<ContextSummaryRecord> =
        dao.getByChatId(chatId).map { it.toRecord() }

    override suspend fun insert(record: ContextSummaryRecord) {
        dao.insert(record.toEntity())
    }

    override suspend fun deleteByChatId(chatId: Long) {
        dao.deleteByChatId(chatId)
    }

    override suspend fun getMaxCoveredIndex(chatId: Long): Int? =
        dao.getMaxCoveredIndex(chatId)
}

private fun ContextSummaryEntity.toRecord() = ContextSummaryRecord(
    id = id,
    chatId = chatId,
    summaryText = summaryText,
    fromMessageIndex = fromMessageIndex,
    toMessageIndex = toMessageIndex,
    originalTokenEstimate = originalTokenEstimate,
    summaryTokenEstimate = summaryTokenEstimate,
)

private fun ContextSummaryRecord.toEntity() = ContextSummaryEntity(
    id = id,
    chatId = chatId,
    summaryText = summaryText,
    fromMessageIndex = fromMessageIndex,
    toMessageIndex = toMessageIndex,
    originalTokenEstimate = originalTokenEstimate,
    summaryTokenEstimate = summaryTokenEstimate,
)
