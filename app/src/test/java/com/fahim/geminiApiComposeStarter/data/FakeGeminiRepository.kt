package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeGeminiRepository : GeminiRepository {

    private val conversationFlow = MutableStateFlow<List<MessageEntity>>(emptyList())
    var simulateError: Boolean = false
    var errorMessageText: String = "Simulated API error"
    var cannedReply: String = "Mock Gemini reply"

    override fun observeConversation(): Flow<List<MessageEntity>> = conversationFlow.asStateFlow()

    override suspend fun sendPrompt(prompt: String): Result<String> {
        val nextId = (conversationFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val userMessage = MessageEntity(id = nextId, content = prompt, fromUser = true)
        conversationFlow.value = conversationFlow.value + userMessage

        if (simulateError) {
            return Result.failure(IllegalStateException(errorMessageText))
        }

        val replyMessage = MessageEntity(id = nextId + 1L, content = cannedReply, fromUser = false)
        conversationFlow.value = conversationFlow.value + replyMessage
        return Result.success(cannedReply)
    }

    override suspend fun generateText(prompt: String): Result<String> {
        return if (simulateError) {
            Result.failure(IllegalStateException(errorMessageText))
        } else {
            Result.success(cannedReply)
        }
    }

    override suspend fun clearConversation() {
        conversationFlow.value = emptyList()
    }
}
