package com.fahim.geminiApiComposeStarter.data

enum class Sender { USER, GEMINI }

data class ChatMessage(
    val id: Long,
    val text: String,
    val sender: Sender,
    val timestamp: Long,
)
