package com.fahim.geminiApiComposeStarter.ui.chat

/** Immutable UI state for the single-screen prompt/response flow. */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
)

enum class PromptError { EMPTY }
