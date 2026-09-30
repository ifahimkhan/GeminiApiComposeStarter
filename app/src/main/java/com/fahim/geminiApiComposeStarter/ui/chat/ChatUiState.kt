package com.fahim.geminiApiComposeStarter.ui.chat

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }