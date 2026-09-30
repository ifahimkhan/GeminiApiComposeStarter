package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.UserPreferences

/** Single source of truth for everything the chat screen renders. */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val preferences: UserPreferences = UserPreferences(),
)

enum class PromptError { EMPTY }
