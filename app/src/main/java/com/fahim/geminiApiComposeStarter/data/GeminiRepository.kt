package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage

interface GeminiRepository {
    suspend fun generateReply(conversation: List<ChatMessage>): Result<String>
}