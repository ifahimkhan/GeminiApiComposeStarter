package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val compactMessages: Boolean = false,
    val historyLoaded: Boolean = false,
    val darkModeOverride: Boolean? = null,
)
