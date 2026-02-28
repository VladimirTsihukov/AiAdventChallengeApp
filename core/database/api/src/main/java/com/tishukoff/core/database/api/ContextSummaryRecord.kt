package com.tishukoff.core.database.api

data class ContextSummaryRecord(
    val id: Long = 0,
    val chatId: Long,
    val summaryText: String,
    val fromMessageIndex: Int,
    val toMessageIndex: Int,
    val originalTokenEstimate: Int,
    val summaryTokenEstimate: Int,
)
