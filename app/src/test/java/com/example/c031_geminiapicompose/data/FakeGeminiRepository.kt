package com.example.c031_geminiapicompose.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeGeminiRepository(
    private val shouldFail: Boolean = false,
    private val fakeResponseText: String = "Hello from fake Gemini!"
) : GeminiRepository {

    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun getMessagesFlow(): Flow<List<ChatMessage>> = _messagesFlow.asStateFlow()

    override suspend fun generateAndSaveResponse(prompt: String): Result<String> {
        val userMsgId = (_messagesFlow.value.maxOfOrNull { it.id } ?: 0) + 1
        val userMsg = ChatMessage(id = userMsgId, text = prompt, isUser = true)
        _messagesFlow.update { it + userMsg }

        if (shouldFail) {
            return Result.failure(RuntimeException("Fake API Network Error"))
        }

        val geminiMsgId = userMsgId + 1
        val geminiMsg = ChatMessage(id = geminiMsgId, text = fakeResponseText, isUser = false)
        _messagesFlow.update { it + geminiMsg }
        return Result.success(fakeResponseText)
    }

    override suspend fun clearHistory() {
        _messagesFlow.value = emptyList()
    }
}
