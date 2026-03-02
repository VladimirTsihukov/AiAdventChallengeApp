package com.tishukoff.core.database.api

data class WorkingMemoryRecord(
    val id: Long = 0,
    val chatId: Long,
    val key: String,
    val value: String,
    val timestamp: Long,
)
