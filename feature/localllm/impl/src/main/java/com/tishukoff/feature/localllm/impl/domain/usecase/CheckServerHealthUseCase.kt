package com.tishukoff.feature.localllm.impl.domain.usecase

import com.tishukoff.feature.localllm.impl.domain.model.ServerStatus
import com.tishukoff.feature.localllm.impl.domain.repository.LocalLlmRepository

/**
 * Проверяет доступность и статус локального LLM-сервера.
 */
internal class CheckServerHealthUseCase(
    private val repository: LocalLlmRepository,
) {

    suspend operator fun invoke(): Result<ServerStatus> = runCatching {
        repository.checkHealth()
    }
}
