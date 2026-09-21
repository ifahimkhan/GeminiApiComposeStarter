package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode

/** Immutable UI state for the whole chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    /** True when the last message is an unanswered prompt, i.e. the request failed and can be retried. */
    val canRetry: Boolean get() = messages.lastOrNull()?.author == Author.USER
}

enum class PromptError { EMPTY }
