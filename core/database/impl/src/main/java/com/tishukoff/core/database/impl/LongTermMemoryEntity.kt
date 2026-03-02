package com.tishukoff.core.database.impl

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "long_term_memory",
    indices = [
        Index(value = ["key"], unique = true),
    ],
)
internal data class LongTermMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String,
    val timestamp: Long,
)
