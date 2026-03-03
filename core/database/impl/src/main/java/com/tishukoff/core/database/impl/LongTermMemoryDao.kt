package com.tishukoff.core.database.impl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LongTermMemoryDao {

    @Query("SELECT * FROM long_term_memory ORDER BY timestamp ASC")
    fun getAll(): Flow<List<LongTermMemoryEntity>>

    @Query("SELECT * FROM long_term_memory ORDER BY timestamp ASC")
    suspend fun getAllOnce(): List<LongTermMemoryEntity>

    @Query("SELECT * FROM long_term_memory WHERE category = :category ORDER BY timestamp ASC")
    fun getByCategory(category: String): Flow<List<LongTermMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LongTermMemoryEntity)

    @Query("DELETE FROM long_term_memory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM long_term_memory")
    suspend fun deleteAll()
}
