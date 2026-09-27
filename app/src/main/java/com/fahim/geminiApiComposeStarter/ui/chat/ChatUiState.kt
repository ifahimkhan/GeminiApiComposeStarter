package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.UserPreferences

/**
 * Single source of truth for the chat screen. Exposed by [ChatViewModel] as a StateFlow;
 * composables only read it and report events back through [ChatActions].
 */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val error: ChatError? = null,
    /** Prompt that failed and can be re-sent with "Retry". */
    val retryPrompt: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val showSettings: Boolean = false,
)

enum class PromptError { EMPTY }

sealed interface ChatError {
    data object MissingApiKey : ChatError
    data object VoiceUnavailable : ChatError
    data class Request(val detail: String?) : ChatError
}
