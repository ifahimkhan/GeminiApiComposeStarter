package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ChatMessage
import com.fahim.geminiApiComposeStarter.data.local.ChatSession

data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: Long? = null,
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val retryAvailable: Boolean = false,
    val displayName: String = DEFAULT_DISPLAY_NAME,
    val settingsDisplayName: String = DEFAULT_DISPLAY_NAME,
    val isSettingsOpen: Boolean = false,
)

enum class PromptError {
    EMPTY,
}

const val DEFAULT_DISPLAY_NAME = "Aryan Paode"
const val STUDENT_ROLL_NUMBER = "C033"
