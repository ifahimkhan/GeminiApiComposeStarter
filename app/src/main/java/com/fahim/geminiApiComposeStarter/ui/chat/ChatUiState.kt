package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

/**
 * Immutable UI state for the Gemini Chat screen.
 */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userName: String = "User",
    val selectedModel: String = "gemini-1.5-flash",
    val tone: String = "Balanced",
    val isSettingsOpen: Boolean = false,
    val hasApiKey: Boolean = true,
)

enum class PromptError {
    EMPTY
}
