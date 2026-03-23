package com.tishukoff.feature.localllm.impl.domain.usecase

import com.tishukoff.feature.localllm.impl.domain.model.LocalLlmMessage
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository

/**
 * Отправляет сообщение в локальную LLM с учётом истории диалога.
 */
internal class SendMessageUseCase(
    private val repository: LocalLlmRepository,
) {

    suspend operator fun invoke(messages: List<LocalLlmMessage>): Result<String> =
        runCatching { repository.sendMessage(messages) }
}
