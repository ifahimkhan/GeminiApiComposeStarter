package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity

/** Immutable UI state for the Gemini chat application. */
data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val promptError: PromptError? = null,
    val isDarkMode: Boolean = false,
    // History drawer
    val chatSessions: List<ChatSession> = emptyList(),
    val isHistoryOpen: Boolean = false,
)

/** Represents a past chat session shown in the history drawer. */
data class ChatSession(
    val sessionId: Long,
    val previewText: String,  // First prompt of the session
)

enum class PromptError { EMPTY }
