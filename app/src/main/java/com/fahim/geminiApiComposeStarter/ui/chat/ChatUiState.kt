package com.fahim.geminiApiComposeStarter.ui.chat

/** One bubble in the conversation. */
data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
)

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }