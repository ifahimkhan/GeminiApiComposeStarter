package com.fahim.geminiApiComposeStarter.data

/** A single bubble in the conversation. [id] is the stable key used by the LazyColumn. */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
)
