package com.fahim.geminiApiComposeStarter.ui.chat

/** Immutable UI state for the chat conversation. */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

/** A single message in the conversation, from either the user or Gemini. */
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
)

enum class PromptError { EMPTY }