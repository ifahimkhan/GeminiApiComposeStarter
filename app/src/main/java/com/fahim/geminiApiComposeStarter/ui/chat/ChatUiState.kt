package com.fahim.geminiApiComposeStarter.ui.chat

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val activeChatId: String = "",
    val chatSummaries: List<ChatSummary> = emptyList(),
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

data class ChatSummary(
    val id: String,
    val title: String,
)

data class ChatMessage(
    val id: Long,
    val text: String,
    val author: ChatAuthor,
)

enum class ChatAuthor { USER, GEMINI }

enum class PromptError { EMPTY }
