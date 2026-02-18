package com.tishukoff.aiadventchallengeapp.data

data class LlmSettings(
    val maxTokens: Int = 1024,
    val temperature: Float = 1.0f,
    val stopSequences: List<String> = emptyList()
)
