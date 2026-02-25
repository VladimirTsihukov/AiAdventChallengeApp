package com.tishukoff.core.database.impl

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
internal data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val metadataText: String?,
    val timestamp: Long
)
