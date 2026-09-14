package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ConversationEntity

data class ChatMessage(
    val id: Long = 0L,
    val text: String,
    val isUser: Boolean,
)

data class ChatUiState(
    val prompt: String = "",
    val response: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<ConversationEntity> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError {
    EMPTY
}