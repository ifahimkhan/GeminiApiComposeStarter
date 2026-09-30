package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.RecentConversation
import java.util.UUID

enum class Participant {
    USER,
    GEMINI
}

enum class AppScreen {
    CHAT,
    SETTINGS
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "",
    val text: String,
    val participant: Participant,
    val timestamp: String = "",
)

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val saveHistory: Boolean = true,
    val speechLanguage: String = "English (United States)",
    val appVersion: String = "1.0.0",
)

/**
 * Immutable UI state for the Gemini Assistant application.
 * Retains all legacy fields (prompt, response, isLoading, promptError, errorMessage)
 * for backward compatibility while extending support for multi-message conversation history,
 * Room persistence, recent conversations, screen navigation, and user preferences.
 */
data class ChatUiState(
    val conversationId: String = "",
    val prompt: String = "",
    val response: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val recentConversations: List<RecentConversation> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val currentScreen: AppScreen = AppScreen.CHAT,
    val settings: SettingsUiState = SettingsUiState(),
)

enum class PromptError { EMPTY }
