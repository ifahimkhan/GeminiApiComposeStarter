package com.devraj.geminiassistant.ui.chat

import com.devraj.geminiassistant.data.local.entity.ChatMessageEntity

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val response: String = "",
    val streamingResponse: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val systemInstruction: String = "You are an intelligent, concise, and helpful AI assistant.",
    val temperature: Float = 0.7f,
    val selectedModel: String = "gemini-3.6-flash",
    val isSettingsOpen: Boolean = false,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val speakingMessageId: Long? = null,
    val lastLatencyMs: Long? = null
)

enum class PromptError { EMPTY }

