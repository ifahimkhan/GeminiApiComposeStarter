package com.fahim.geminiApiComposeStarter.ui.chat

import java.util.UUID

enum class MessageStatus { SENT, SUCCESS, ERROR, LOADING }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SUCCESS,
)
