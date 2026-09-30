package com.fahim.geminiApiComposeStarter.data

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessage(
    val id: Long,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
)
