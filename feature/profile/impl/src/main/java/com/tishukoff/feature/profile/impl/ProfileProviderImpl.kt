package com.tishukoff.feature.profile.impl

import android.content.SharedPreferences
import com.tishukoff.feature.profile.api.ProfileProvider
import com.tishukoff.feature.profile.api.models.PreferredFormat
import com.tishukoff.feature.profile.api.models.ProfilePreset
import com.tishukoff.feature.profile.api.models.ResponseLength
import com.tishukoff.feature.profile.api.models.ResponseStyle
import com.tishukoff.feature.profile.api.models.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

internal class ProfileProviderImpl(
    private val prefs: SharedPreferences,
) : ProfileProvider {

    private val _currentProfile = MutableStateFlow(loadProfile())
    override val currentProfile: Flow<UserProfile> = _currentProfile.asStateFlow()

    override fun buildProfilePrompt(): String {
        val profile = _currentProfile.value
        if (profile.name.isBlank() && profile.profession.isBlank() && profile.customInstructions.isBlank()) {
            return ""
        }
        return buildString {
            appendLine("## User Profile")
            if (profile.name.isNotBlank()) appendLine("Name: ${profile.name}")
            if (profile.profession.isNotBlank()) appendLine("Profession: ${profile.profession}")

            appendLine()
            appendLine("## Response Preferences")
            appendLine("Style: ${profile.responseStyle.description}")
            appendLine("Length: ${profile.responseLength.description}")
            appendLine("Format: ${profile.preferredFormat.description}")
            appendLine("Language: ${profile.language}")

            if (profile.restrictions.isNotEmpty()) {
                appendLine()
                appendLine("## Restrictions")
                profile.restrictions.forEach { appendLine("- $it") }
            }

            if (profile.customInstructions.isNotBlank()) {
                appendLine()
                appendLine("## Custom Instructions")
                appendLine(profile.customInstructions)
            }
        }.trimEnd()
    }

    override fun updateProfile(profile: UserProfile) {
        saveProfile(profile)
        _currentProfile.value = profile
    }

    override fun applyPreset(preset: ProfilePreset) {
        updateProfile(preset.profile)
    }

    private fun loadProfile(): UserProfile {
        val json = prefs.getString(KEY_PROFILE, null) ?: return UserProfile()
        return try {
            val obj = JSONObject(json)
            UserProfile(
                name = obj.optString("name", ""),
                profession = obj.optString("profession", ""),
                responseStyle = ResponseStyle.entries.find { it.name == obj.optString("responseStyle") }
                    ?: ResponseStyle.CASUAL,
                responseLength = ResponseLength.entries.find { it.name == obj.optString("responseLength") }
                    ?: ResponseLength.MODERATE,
                preferredFormat = PreferredFormat.entries.find { it.name == obj.optString("preferredFormat") }
                    ?: PreferredFormat.STRUCTURED,
                language = obj.optString("language", "Русский"),
                restrictions = obj.optJSONArray("restrictions")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                customInstructions = obj.optString("customInstructions", ""),
            )
        } catch (_: Exception) {
            UserProfile()
        }
    }

    private fun saveProfile(profile: UserProfile) {
        val json = JSONObject().apply {
            put("name", profile.name)
            put("profession", profile.profession)
            put("responseStyle", profile.responseStyle.name)
            put("responseLength", profile.responseLength.name)
            put("preferredFormat", profile.preferredFormat.name)
            put("language", profile.language)
            put("restrictions", JSONArray(profile.restrictions))
            put("customInstructions", profile.customInstructions)
        }
        prefs.edit().putString(KEY_PROFILE, json.toString()).apply()
    }

    private companion object {
        const val KEY_PROFILE = "user_profile"
    }
}
