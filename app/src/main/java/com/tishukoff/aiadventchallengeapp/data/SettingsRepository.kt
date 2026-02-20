package com.tishukoff.aiadventchallengeapp.data

import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsRepository(private val prefs: SharedPreferences) {

    fun load(): LlmSettings {
        return LlmSettings(
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 1024),
            temperature = prefs.getFloat(KEY_TEMPERATURE, 1.0f),
            stopSequences = prefs.getString(KEY_STOP_SEQUENCES, "")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList(),
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, "") ?: ""
        )
    }

    fun save(settings: LlmSettings) {
        prefs.edit {
            putInt(KEY_MAX_TOKENS, settings.maxTokens)
                .putFloat(KEY_TEMPERATURE, settings.temperature)
                .putString(KEY_STOP_SEQUENCES, settings.stopSequences.joinToString(","))
                .putString(KEY_SYSTEM_PROMPT, settings.systemPrompt)
        }
    }

    private companion object {
        const val KEY_MAX_TOKENS = "llm_max_tokens"
        const val KEY_TEMPERATURE = "llm_temperature"
        const val KEY_STOP_SEQUENCES = "llm_stop_sequences"
        const val KEY_SYSTEM_PROMPT = "llm_system_prompt"
    }
}
