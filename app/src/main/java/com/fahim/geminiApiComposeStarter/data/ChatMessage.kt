package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity

data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false
)

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    text = text,
    isFromUser = isFromUser,
    isError = isError
)
