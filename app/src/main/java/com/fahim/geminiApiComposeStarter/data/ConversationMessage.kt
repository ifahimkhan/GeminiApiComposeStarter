package com.fahim.geminiApiComposeStarter.data

data class ConversationMessage(
    val role: ConversationRole,
    val text: String,
)

enum class ConversationRole { USER, MODEL }
