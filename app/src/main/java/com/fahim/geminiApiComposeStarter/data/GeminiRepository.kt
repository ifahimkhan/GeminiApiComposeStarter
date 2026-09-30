package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

interface GeminiRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun generateText(prompt: String, modelName: String): Result<String>
    suspend fun clearChat()
    suspend fun addMessage(text: String, isFromUser: Boolean, isError: Boolean = false)
}
