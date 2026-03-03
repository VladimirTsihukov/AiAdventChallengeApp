package com.tishukoff.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tishukoff.feature.profile.api.ProfileProvider
import com.tishukoff.feature.profile.api.models.PreferredFormat
import com.tishukoff.feature.profile.api.models.ProfilePreset
import com.tishukoff.feature.profile.api.models.ResponseLength
import com.tishukoff.feature.profile.api.models.ResponseStyle
import com.tishukoff.feature.profile.api.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ProfileViewModel(
    private val profileProvider: ProfileProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        profileProvider.currentProfile
            .onEach { profile ->
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    selectedPreset = findMatchingPreset(profile),
                    isSaved = false,
                )
            }
            .launchIn(viewModelScope)
    }

    fun updateName(name: String) {
        updateProfile { copy(name = name) }
    }

    fun updateProfession(profession: String) {
        updateProfile { copy(profession = profession) }
    }

    fun updateLanguage(language: String) {
        updateProfile { copy(language = language) }
    }

    fun updateCustomInstructions(instructions: String) {
        updateProfile { copy(customInstructions = instructions) }
    }

    fun updateResponseStyle(style: ResponseStyle) {
        updateProfile { copy(responseStyle = style) }
    }

    fun updateResponseLength(length: ResponseLength) {
        updateProfile { copy(responseLength = length) }
    }

    fun updatePreferredFormat(format: PreferredFormat) {
        updateProfile { copy(preferredFormat = format) }
    }

    fun addRestriction(restriction: String) {
        if (restriction.isBlank()) return
        updateProfile { copy(restrictions = restrictions + restriction.trim()) }
    }

    fun removeRestriction(index: Int) {
        updateProfile {
            copy(restrictions = restrictions.filterIndexed { i, _ -> i != index })
        }
    }

    fun applyPreset(preset: ProfilePreset) {
        profileProvider.applyPreset(preset)
        _uiState.value = _uiState.value.copy(
            profile = preset.profile,
            selectedPreset = preset,
            isSaved = true,
        )
    }

    fun save() {
        profileProvider.updateProfile(_uiState.value.profile)
        _uiState.value = _uiState.value.copy(isSaved = true)
    }

    private fun updateProfile(update: UserProfile.() -> UserProfile) {
        val newProfile = _uiState.value.profile.update()
        _uiState.value = _uiState.value.copy(
            profile = newProfile,
            selectedPreset = findMatchingPreset(newProfile),
            isSaved = false,
        )
    }

    private fun findMatchingPreset(profile: UserProfile): ProfilePreset? =
        ProfilePreset.entries.find { it.profile == profile }
}
