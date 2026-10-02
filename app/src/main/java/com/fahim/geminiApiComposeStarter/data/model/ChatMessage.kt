package com.fahim.geminiApiComposeStarter.data.model

import androidx.compose.runtime.Immutable

enum class Role { USER, MODEL }

/** One turn of the conversation. [id] is stable and used as the LazyColumn key. */
@Immutable
data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val timestamp: Long,
)
