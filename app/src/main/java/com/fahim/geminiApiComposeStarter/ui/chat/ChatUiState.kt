package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.Conversation
import com.fahim.geminiApiComposeStarter.data.local.Message

data class ChatUiState(
    val prompt: String = "",
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val errorMessage: String? = null,
    val messages: List<Message> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: Long? = null,
    val currentBranchId: String = "main",
    val branches: List<String> = listOf("main"),
    val responseLengthPreference: String = "Normal"
)
