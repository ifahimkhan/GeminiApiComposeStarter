package com.example.myapplication

import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatRole
import com.example.myapplication.data.repository.GeminiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeGeminiRepository : GeminiRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    var shouldReturnError = false

    override fun getChatHistory(): Flow<List<ChatMessage>> {
        return _messages.asStateFlow()
    }

    override suspend fun sendMessage(prompt: String): Result<String> {
        val userMsg = ChatMessage(role = ChatRole.USER, text = prompt)
        _messages.update { it + userMsg }

        return if (shouldReturnError) {
            Result.failure(Exception("Fake network error"))
        } else {
            val responseText = "Echo: $prompt"
            val modelMsg = ChatMessage(role = ChatRole.MODEL, text = responseText)
            _messages.update { it + modelMsg }
            Result.success(responseText)
        }
    }

    override suspend fun clearHistory() {
        _messages.update { emptyList() }
    }
}
