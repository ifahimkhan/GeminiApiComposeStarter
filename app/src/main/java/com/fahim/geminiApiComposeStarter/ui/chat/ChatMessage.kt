package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import java.util.UUID

enum class ChatRole {
    USER,
    ASSISTANT,
}

/**
 * Immutable message model representing a single message turn in the conversation.
 */
@Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val role: ChatRole,
    val timestamp: Long = System.currentTimeMillis(),
)
