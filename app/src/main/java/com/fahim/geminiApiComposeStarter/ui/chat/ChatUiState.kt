package com.fahim.geminiApiComposeStarter.ui.chat

enum class Author { USER, GEMINI }

/** One chat bubble. [id] is stable and unique, so it doubles as the LazyColumn key. */
data class ChatMessage(
    val id: Long,
    val author: Author,
    val text: String,
)

/** Immutable UI state for the whole chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
) {
    /** True when the last message is an unanswered prompt, i.e. the request failed and can be retried. */
    val canRetry: Boolean get() = messages.lastOrNull()?.author == Author.USER
}

enum class PromptError { EMPTY }
