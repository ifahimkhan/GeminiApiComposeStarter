package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode

/** Immutable UI state for the whole chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    /** Picture waiting to be sent with the next prompt (a file in app storage). */
    val attachedImagePath: String? = null,
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    /** True when [errorMessage] came from a failed Gemini request, so a Retry action makes sense. */
    val errorIsRetryable: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Id of the reply being read aloud, or null. */
    val speakingMessageId: Long? = null,
) {
    /** True when the last message is an unanswered prompt, i.e. the request failed and can be retried. */
    val canRetry: Boolean get() = messages.lastOrNull()?.author == Author.USER
}

enum class PromptError { EMPTY }
