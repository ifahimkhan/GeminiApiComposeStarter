package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.db.ConversationMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeGeminiRepository : GeminiRepository {

    private val messagesFlow = MutableStateFlow<List<ConversationMessage>>(emptyList())
    var shouldFail: Boolean = false
    var failureMessage: String = "Network Error"
    var generatedResponseText: String = "Fake Gemini Response"

    override fun getChatHistory(): Flow<List<ConversationMessage>> = messagesFlow.asStateFlow()

    override suspend fun sendMessage(prompt: String): Result<String> {
        val nextId = (messagesFlow.value.maxOfOrNull { it.id } ?: 0L) + 1
        val userMsg = ConversationMessage(id = nextId, content = prompt, isUserSent = true)
        messagesFlow.value = messagesFlow.value + userMsg

        if (shouldFail) {
            return Result.failure(Exception(failureMessage))
        }

        val aiMsg = ConversationMessage(id = nextId + 1, content = generatedResponseText, isUserSent = false)
        messagesFlow.value = messagesFlow.value + aiMsg
        return Result.success(generatedResponseText)
    }

    override suspend fun generateText(prompt: String): Result<String> {
        if (shouldFail) return Result.failure(Exception(failureMessage))
        return Result.success(generatedResponseText)
    }

    override suspend fun clearHistory() {
        messagesFlow.value = emptyList()
    }
}
