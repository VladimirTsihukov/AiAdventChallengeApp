package com.tishukoff.feature.profile.api

import com.tishukoff.feature.profile.api.models.ProfilePreset
import com.tishukoff.feature.profile.api.models.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Provides access to the user profile for personalization.
 */
interface ProfileProvider {

    /** Current user profile as a reactive stream. */
    val currentProfile: Flow<UserProfile>

    /** Builds a system prompt section from the current profile. Returns empty string if profile is empty. */
    fun buildProfilePrompt(): String

    /** Updates the user profile. */
    fun updateProfile(profile: UserProfile)

    /** Applies a preset profile. */
    fun applyPreset(preset: ProfilePreset)
}
