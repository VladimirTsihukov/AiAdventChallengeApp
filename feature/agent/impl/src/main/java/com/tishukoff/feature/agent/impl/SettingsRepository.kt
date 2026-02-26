package com.tishukoff.feature.agent.impl

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tishukoff.feature.agent.api.ClaudeModel
import com.tishukoff.feature.agent.api.CompressionSettings
import com.tishukoff.feature.agent.api.LlmSettings

internal class SettingsRepository(private val prefs: SharedPreferences) {

    fun load(): LlmSettings {
        val modelName = prefs.getString(KEY_MODEL, ClaudeModel.SONNET.name) ?: ClaudeModel.SONNET.name
        val model = ClaudeModel.entries.find { it.name == modelName } ?: ClaudeModel.SONNET
        return LlmSettings(
            model = model,
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 1024),
            temperature = prefs.getFloat(KEY_TEMPERATURE, 1.0f),
            stopSequences = prefs.getString(KEY_STOP_SEQUENCES, "")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList(),
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "") ?: "",
            compression = CompressionSettings(
                enabled = prefs.getBoolean(KEY_COMPRESSION_ENABLED, false),
                recentMessagesToKeep = prefs.getInt(KEY_COMPRESSION_RECENT, 10),
                summarizationBatchSize = prefs.getInt(KEY_COMPRESSION_BATCH_SIZE, 10),
            ),
        )
    }

    fun save(settings: LlmSettings) {
        prefs.edit {
            putString(KEY_MODEL, settings.model.name)
                .putInt(KEY_MAX_TOKENS, settings.maxTokens)
                .putFloat(KEY_TEMPERATURE, settings.temperature)
                .putString(KEY_STOP_SEQUENCES, settings.stopSequences.joinToString(","))
                .putString(KEY_SYSTEM_PROMPT, settings.systemPrompt)
                .putBoolean(KEY_COMPRESSION_ENABLED, settings.compression.enabled)
                .putInt(KEY_COMPRESSION_RECENT, settings.compression.recentMessagesToKeep)
                .putInt(KEY_COMPRESSION_BATCH_SIZE, settings.compression.summarizationBatchSize)
        }
    }

    private companion object {
        const val KEY_MODEL = "llm_model"
        const val KEY_MAX_TOKENS = "llm_max_tokens"
        const val KEY_TEMPERATURE = "llm_temperature"
        const val KEY_STOP_SEQUENCES = "llm_stop_sequences"
        const val KEY_SYSTEM_PROMPT = "llm_system_prompt"
        const val KEY_COMPRESSION_ENABLED = "compression.enabled"
        const val KEY_COMPRESSION_RECENT = "compression.recentMessagesToKeep"
        const val KEY_COMPRESSION_BATCH_SIZE = "compression.summarizationBatchSize"
    }
}
