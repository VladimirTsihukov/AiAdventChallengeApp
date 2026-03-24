package com.tishukoff.feature.localllm.impl.domain.model

/**
 * Сообщение в чате с локальной LLM.
 */
data class LocalLlmMessage(
    val text: String,
    val role: Role,
) {
    enum class Role { USER, ASSISTANT }
}
