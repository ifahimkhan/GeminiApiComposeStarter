package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

fun ChatMessageEntity.toUiModel(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    isUser = isUser,
    timestamp = timestamp,
    isError = isError
)

data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val isVoiceActive: Boolean = false
)

enum class PromptError { EMPTY }
