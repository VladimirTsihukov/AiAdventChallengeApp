package com.tishukoff.core.database.api

/**
 * Persistent record representing a single chat conversation.
 */
data class ChatRecord(
    val id: Long,
    val title: String,
    val createdAt: Long,
)
