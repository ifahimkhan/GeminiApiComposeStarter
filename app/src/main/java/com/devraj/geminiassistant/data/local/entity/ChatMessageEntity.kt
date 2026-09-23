package com.devraj.geminiassistant.data.local.entity

data class ChatMessageEntity(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)
