package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: Boolean = false,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
