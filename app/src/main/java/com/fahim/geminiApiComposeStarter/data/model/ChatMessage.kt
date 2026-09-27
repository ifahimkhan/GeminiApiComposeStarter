package com.fahim.geminiApiComposeStarter.data.model

import androidx.compose.runtime.Immutable

enum class Role { USER, MODEL }

/** One bubble in the conversation. [id] is the Room primary key and the LazyColumn key. */
@Immutable
data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val timestamp: Long,
)
