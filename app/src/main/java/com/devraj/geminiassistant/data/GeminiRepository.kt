package com.devraj.geminiassistant.data

import com.devraj.geminiassistant.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

interface GeminiRepository {
    fun getMessagesFlow(): Flow<List<ChatMessageEntity>>
    suspend fun generateText(prompt: String): Result<String>
    fun generateStream(prompt: String): Flow<String>
    suspend fun clearHistory()
    fun getSystemInstructionFlow(): Flow<String>
    fun getTemperatureFlow(): Flow<Float>
    fun getSelectedModelFlow(): Flow<String>
    suspend fun updatePreferences(instruction: String, temperature: Float, model: String = "gemini-3.6-flash")
}

