package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity

/** Immutable UI state for the AI Chat application. */
data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val themePreference: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val isVoiceEnabled: Boolean = true,
    val hasApiKey: Boolean = true,
)

enum class PromptError { EMPTY }
