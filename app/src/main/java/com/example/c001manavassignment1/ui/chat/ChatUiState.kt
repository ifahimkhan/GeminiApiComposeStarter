package com.example.c001manavassignment1.ui.chat

import com.example.c001manavassignment1.data.GeminiRepository
import com.example.c001manavassignment1.data.local.ConversationEntity

data class ChatUiState(
    val messages: List<GeminiRepository.ChatMessage> = emptyList(),
    val conversations: List<ConversationEntity> = emptyList(),
    val activeConversationId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val isDarkMode: Boolean = false
)
