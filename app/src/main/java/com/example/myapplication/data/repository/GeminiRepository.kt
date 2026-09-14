package com.example.myapplication.data.repository

import com.example.myapplication.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface GeminiRepository {
    fun getChatHistory(): Flow<List<ChatMessage>>
    suspend fun sendMessage(prompt: String): Result<String>
    suspend fun clearHistory()
}
