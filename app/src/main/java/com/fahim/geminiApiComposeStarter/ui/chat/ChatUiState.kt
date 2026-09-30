package com.fahim.geminiApiComposeStarter.ui.chat

/** One chat message. [id] is used as a stable key in the LazyColumn. */
data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
)

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    /** Saved in DataStore. null = follow the phone's dark mode setting. */
    val darkMode: Boolean? = null,
)

enum class PromptError { EMPTY }