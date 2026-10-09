package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.AppPreferences

/** Single source of truth for everything the chat screen renders. */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Prompt that failed, so the user can retry it. Null for non-retryable errors. */
    val failedPrompt: String? = null,
    val settings: AppPreferences = AppPreferences(),
    val isSettingsOpen: Boolean = false,
) {
    val canSend: Boolean get() = input.isNotBlank() && !isLoading
}
