package com.tishukoff.core.database.impl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
internal data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
)
