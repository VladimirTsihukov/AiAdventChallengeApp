package com.tishukoff.feature.memory.api

data class MemoryEntry(
    val id: Long = 0,
    val key: String,
    val value: String,
    val type: MemoryType,
    val category: String? = null,
    val chatId: Long? = null,
    val timestamp: Long = 0,
)
