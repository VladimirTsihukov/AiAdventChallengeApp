package com.tishukoff.feature.agent.api

enum class ContextStrategyType(val displayName: String) {
    SUMMARIZATION("Summarization"),
    SLIDING_WINDOW("Sliding Window"),
    STICKY_FACTS("Sticky Facts"),
    BRANCHING("Branching"),
}
