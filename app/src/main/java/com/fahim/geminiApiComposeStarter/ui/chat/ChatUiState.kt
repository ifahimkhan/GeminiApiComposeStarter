package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode

/** A single chat bubble, either from the user or from Gemini. */
data class ChatMessage(
    val id: Long,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
)

/** One entry in the sidebar's conversation history list. */
data class ConversationSummary(
    val id: Long,
    val title: String,
    val updatedAt: Long,
)

enum class PromptError { EMPTY }

/** Immutable UI state for the chat screen, its sidebar and its settings dialog. */
data class ChatUiState(
    val isReady: Boolean = false,
    val username: String? = null,
    val conversations: List<ConversationSummary> = emptyList(),
    val currentConversationId: Long? = null,
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val promptError: PromptError? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val showSettingsDialog: Boolean = false,
    val maskedApiKey: String = "",
    val isUsingCustomKey: Boolean = false,
    val hasApiKey: Boolean = false,
) {
    val currentConversationTitle: String
        get() = conversations.firstOrNull { it.id == currentConversationId }?.title ?: "New chat"
}
