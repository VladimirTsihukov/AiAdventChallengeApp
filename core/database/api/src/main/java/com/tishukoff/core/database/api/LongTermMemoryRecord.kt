package com.tishukoff.core.database.api

data class LongTermMemoryRecord(
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String,
    val timestamp: Long,
)
