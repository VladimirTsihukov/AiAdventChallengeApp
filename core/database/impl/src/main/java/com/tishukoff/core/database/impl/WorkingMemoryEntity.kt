package com.tishukoff.core.database.impl

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "working_memory",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("chatId"),
        Index(value = ["chatId", "key"], unique = true),
    ],
)
internal data class WorkingMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val key: String,
    val value: String,
    val timestamp: Long,
)
