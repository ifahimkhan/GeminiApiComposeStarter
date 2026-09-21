package com.fahim.geminiApiComposeStarter.ui.chat

enum class ChatRole { USER, MODEL }

/** A single turn in the conversation, stable-keyed by [id] for LazyColumn diffing. */
data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val timestamp: Long,
)
