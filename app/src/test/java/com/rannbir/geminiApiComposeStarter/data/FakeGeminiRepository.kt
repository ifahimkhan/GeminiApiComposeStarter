package com.rannbir.geminiApiComposeStarter.data

import com.rannbir.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeGeminiRepository : GeminiRepository {

    private val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    var shouldFail: Boolean = false
    var stubbedResponse: String = "This is a response from Fake Gemini"

    override fun getChatMessages(): Flow<List<ChatMessageEntity>> = messagesFlow.asStateFlow()

    override suspend fun sendMessage(prompt: String): Result<String> {
        if (shouldFail) {
            return Result.failure(RuntimeException("Simulated Gemini API error"))
        }

        val currentList = messagesFlow.value.toMutableList()
        val userMsg = ChatMessageEntity(
            id = (currentList.size + 1).toLong(),
            text = prompt,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        currentList.add(userMsg)

        val geminiMsg = ChatMessageEntity(
            id = (currentList.size + 1).toLong(),
            text = stubbedResponse,
            isFromUser = false,
            timestamp = System.currentTimeMillis() + 1
        )
        currentList.add(geminiMsg)

        messagesFlow.value = currentList
        return Result.success(stubbedResponse)
    }

    override suspend fun clearChatHistory() {
        messagesFlow.value = emptyList()
    }
}
