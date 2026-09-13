package com.example.c031_geminiapicompose.data

import kotlinx.coroutines.flow.Flow

interface GeminiRepository {
    fun getMessagesFlow(): Flow<List<ChatMessage>>
    suspend fun generateAndSaveResponse(prompt: String): Result<String>
    suspend fun clearHistory()
}
