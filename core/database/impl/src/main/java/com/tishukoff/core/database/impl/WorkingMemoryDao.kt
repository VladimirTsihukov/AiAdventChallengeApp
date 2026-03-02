package com.tishukoff.core.database.impl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface WorkingMemoryDao {

    @Query("SELECT * FROM working_memory WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getByChatId(chatId: Long): Flow<List<WorkingMemoryEntity>>

    @Query("SELECT * FROM working_memory WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getByChatIdOnce(chatId: Long): List<WorkingMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkingMemoryEntity)

    @Query("DELETE FROM working_memory WHERE chatId = :chatId")
    suspend fun deleteByChatId(chatId: Long)

    @Query("DELETE FROM working_memory WHERE chatId = :chatId AND `key` = :key")
    suspend fun deleteByKey(chatId: Long, key: String)
}
