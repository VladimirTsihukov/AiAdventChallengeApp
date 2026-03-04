package com.tishukoff.feature.profile.api.models

data class UserProfile(
    val name: String = "",
    val profession: String = "",
    val responseStyle: ResponseStyle = ResponseStyle.CASUAL,
    val responseLength: ResponseLength = ResponseLength.MODERATE,
    val preferredFormat: PreferredFormat = PreferredFormat.STRUCTURED,
    val language: String = "Русский",
    val restrictions: List<String> = emptyList(),
    val customInstructions: String = "",
)
