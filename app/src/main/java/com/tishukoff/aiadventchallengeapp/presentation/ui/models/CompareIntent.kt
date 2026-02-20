package com.tishukoff.aiadventchallengeapp.presentation.ui.models

sealed interface CompareIntent {
    data class UpdatePrompt(val text: String) : CompareIntent
    data object Compare : CompareIntent
}
