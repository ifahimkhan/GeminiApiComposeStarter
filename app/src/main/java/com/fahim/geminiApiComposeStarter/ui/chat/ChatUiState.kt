package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatEntity

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val messages: List<ChatEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVoiceInputActive: Boolean = false
)
