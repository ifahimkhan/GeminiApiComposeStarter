package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode

data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: Long? = null,
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val compactBubbles: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

enum class PromptError { EMPTY }

enum class ChatAuthor { USER, GEMINI }

data class ChatMessage(
    val id: Long,
    val text: String,
    val author: ChatAuthor,
    val timestamp: Long = 0L,
)

data class Conversation(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)