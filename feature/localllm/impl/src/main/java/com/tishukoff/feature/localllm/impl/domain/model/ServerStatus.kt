package com.tishukoff.feature.localllm.impl.domain.model

/**
 * Статус локального LLM-сервера.
 *
 * @param isOnline доступен ли сервер
 * @param version версия Ollama
 * @param models список доступных моделей
 * @param responseTimeMs время отклика в миллисекундах
 */
data class ServerStatus(
    val isOnline: Boolean,
    val version: String = "",
    val models: List<ModelInfo> = emptyList(),
    val responseTimeMs: Long = 0,
)

/**
 * Информация о модели Ollama.
 *
 * @param name название модели
 * @param size размер модели в байтах
 */
data class ModelInfo(
    val name: String,
    val size: Long = 0,
)
