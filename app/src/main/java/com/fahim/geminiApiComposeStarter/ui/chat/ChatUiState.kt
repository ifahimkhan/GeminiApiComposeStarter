package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode

data class ChatUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedModel: String = "gemini-3.6-flash",
    val inputText: String = "",
    val isSending: Boolean = false,
    val isListening: Boolean = false,
    val userName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val modelNoticeDialogMessage: String? = null
)
