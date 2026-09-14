package com.example.myapplication.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
