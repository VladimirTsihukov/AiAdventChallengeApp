package com.tishukoff.feature.rag.impl.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface RagChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<RagChunkEntity>)

    @Query("SELECT * FROM rag_chunks WHERE strategy = :strategy")
    suspend fun getChunksByStrategy(strategy: String): List<RagChunkEntity>

    @Query("SELECT COUNT(*) FROM rag_chunks WHERE strategy = :strategy")
    suspend fun getChunkCount(strategy: String): Int

    @Query("DELETE FROM rag_chunks WHERE strategy = :strategy")
    suspend fun deleteByStrategy(strategy: String)

    @Query("DELETE FROM rag_chunks")
    suspend fun deleteAll()
}
