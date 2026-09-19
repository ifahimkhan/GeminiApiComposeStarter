package com.fahim.geminiApiComposeStarter.ui.chat

/** Immutable UI state for the single-screen prompt/response flow. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val isDarkMode: Boolean = false,
)

enum class PromptError { EMPTY }
