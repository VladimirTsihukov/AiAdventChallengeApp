package com.tishukoff.feature.profile.api.models

enum class ProfilePreset(val displayName: String, val profile: UserProfile) {
    DEVELOPER(
        displayName = "Developer",
        profile = UserProfile(
            name = "Developer",
            profession = "Software Engineer",
            responseStyle = ResponseStyle.CASUAL,
            responseLength = ResponseLength.MODERATE,
            preferredFormat = PreferredFormat.CODE_FOCUSED,
            language = "Русский",
            restrictions = emptyList(),
            customInstructions = "Предпочитаю примеры кода на Kotlin. Используй технические термины без упрощений.",
        ),
    ),
    STUDENT(
        displayName = "Student",
        profile = UserProfile(
            name = "Student",
            profession = "Студент",
            responseStyle = ResponseStyle.FRIENDLY,
            responseLength = ResponseLength.DETAILED,
            preferredFormat = PreferredFormat.STRUCTURED,
            language = "Русский",
            restrictions = emptyList(),
            customInstructions = "Объясняй простым языком с примерами. Если есть сложные термины — давай определения.",
        ),
    ),
    BUSINESS(
        displayName = "Business",
        profile = UserProfile(
            name = "Business Manager",
            profession = "Менеджер проектов",
            responseStyle = ResponseStyle.FORMAL,
            responseLength = ResponseLength.BRIEF,
            preferredFormat = PreferredFormat.STRUCTURED,
            language = "Русский",
            restrictions = emptyList(),
            customInstructions = "Фокусируйся на бизнес-ценности и ROI. Избегай технических деталей реализации.",
        ),
    ),
    CUSTOM(
        displayName = "Custom",
        profile = UserProfile(),
    ),
}
