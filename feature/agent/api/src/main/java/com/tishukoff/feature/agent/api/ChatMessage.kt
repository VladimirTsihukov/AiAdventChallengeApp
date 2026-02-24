package com.tishukoff.feature.agent.api

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val metadataText: String? = null
)
