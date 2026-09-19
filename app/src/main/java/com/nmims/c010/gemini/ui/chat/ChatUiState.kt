package com.nmims.c010.gemini.ui.chat

import androidx.compose.runtime.Immutable
import com.nmims.c010.gemini.data.ChatMessage

@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isListening: Boolean = false
) {
    val canSend: Boolean get() = input.isNotBlank() && !isLoading
    val isEmpty: Boolean get() = messages.isEmpty() && !isLoading
}
