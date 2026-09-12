package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.AppTheme

/** Immutable UI state for the rich conversation chat flow. */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val selectedTheme: AppTheme = AppTheme.SYSTEM,
)

enum class PromptError { EMPTY }
