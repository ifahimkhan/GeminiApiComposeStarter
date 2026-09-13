
package com.fahim.geminiApiComposeStarter.ui.chat

/**
 * A single message in the conversation.
 *
 * Each message has a stable ID so LazyColumn can track it correctly.
 */
data class ChatMessage(
    val id: Long,
    val text: String,
    val isFromUser: Boolean,
)

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val prompt: String = "",
    val response: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }