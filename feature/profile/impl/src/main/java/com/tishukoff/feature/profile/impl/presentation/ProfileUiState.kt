package com.tishukoff.feature.profile.impl.presentation

import com.tishukoff.feature.profile.api.models.ProfilePreset
import com.tishukoff.feature.profile.api.models.UserProfile

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val selectedPreset: ProfilePreset? = null,
    val isSaved: Boolean = false,
)
