package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeGeminiRepository : GeminiRepository {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    var shouldFail: Boolean = false
    var failureException: Exception = Exception("Network error occurred")
    var responseText: String = "Test response from Gemini"

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            Result.success(responseText)
        }
    }

    override fun getChatHistory(): Flow<List<ChatMessage>> = messages.asStateFlow()

    override suspend fun saveMessage(message: ChatMessage) {
        messages.value = messages.value + message
    }

    override suspend fun clearHistory() {
        messages.value = emptyList()
    }
}
