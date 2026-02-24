package com.tishukoff.feature.agent.api

data class LlmSettings(
    val model: ClaudeModel = ClaudeModel.SONNET,
    val maxTokens: Int = 1024,
    val temperature: Float = 1.0f,
    val stopSequences: List<String> = emptyList(),
    val systemPrompt: String = ""
)
