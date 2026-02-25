package com.tishukoff.core.database.impl

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ChatEntity>>

    @Insert
    suspend fun insert(chat: ChatEntity): Long

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun delete(chatId: Long)

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateTitle(chatId: Long, title: String)
}
