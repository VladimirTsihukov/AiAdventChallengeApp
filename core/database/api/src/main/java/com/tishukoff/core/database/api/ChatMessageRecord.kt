package com.tishukoff.core.database.api

/**
 * Persistent record representing a single chat message.
 */
data class ChatMessageRecord(
    val text: String,
    val isUser: Boolean,
    val metadataText: String? = null
)
