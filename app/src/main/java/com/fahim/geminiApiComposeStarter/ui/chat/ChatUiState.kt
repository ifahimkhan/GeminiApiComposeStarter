package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode

/**
 * Everything the chat screen needs to draw itself, and nothing else. Held as a single
 * immutable value so composables stay stateless and Compose can skip unchanged subtrees.
 */
@Immutable
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val error: ErrorEvent? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColour: Boolean = true,
) {
    val isConversationEmpty: Boolean get() = messages.isEmpty() && !isLoading
}

enum class PromptError { EMPTY }

/**
 * A one-shot error for the snackbar. [id] makes two identical failures distinct values, so
 * the second one still re-triggers the LaunchedEffect that shows it.
 */
@Immutable
data class ErrorEvent(
    val id: Long,
    val message: String,
    val retryable: Boolean,
)
