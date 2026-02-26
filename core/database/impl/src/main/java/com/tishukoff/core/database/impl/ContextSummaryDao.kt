package com.tishukoff.core.database.impl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface ContextSummaryDao {

    @Query("SELECT * FROM context_summaries WHERE chatId = :chatId ORDER BY fromMessageIndex ASC")
    suspend fun getByChatId(chatId: Long): List<ContextSummaryEntity>

    @Insert
    suspend fun insert(entity: ContextSummaryEntity)

    @Query("DELETE FROM context_summaries WHERE chatId = :chatId")
    suspend fun deleteByChatId(chatId: Long)

    @Query("SELECT MAX(toMessageIndex) FROM context_summaries WHERE chatId = :chatId")
    suspend fun getMaxCoveredIndex(chatId: Long): Int?
}
