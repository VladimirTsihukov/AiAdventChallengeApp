package com.tishukoff.core.database.impl

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_summaries",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("chatId")],
)
internal data class ContextSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val summaryText: String,
    val fromMessageIndex: Int,
    val toMessageIndex: Int,
    val originalTokenEstimate: Int,
    val summaryTokenEstimate: Int,
)
