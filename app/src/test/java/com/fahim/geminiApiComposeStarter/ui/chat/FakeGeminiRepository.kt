package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake repository for unit testing ChatViewModel. */
class FakeGeminiRepository(
    private val responseResult: Result<String> = Result.success("Copy that. DRS enables a 10-15 km/h speed boost on straights.")
) : GeminiRepository {

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    var generateCallCount = 0
    var lastPrompt: String? = null
    var shouldDelayResponse = false

    override suspend fun generateText(prompt: String): Result<String> {
        generateCallCount++
        lastPrompt = prompt
        if (shouldDelayResponse) {
            kotlinx.coroutines.delay(1000)
        }
        return responseResult
    }

    override fun getMessages(): Flow<List<ChatMessageEntity>> = _messages.asStateFlow()

    override suspend fun saveMessage(message: ChatMessageEntity): Long {
        val newId = (_messages.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val withId = message.copy(id = newId)
        _messages.value = _messages.value + withId
        return newId
    }

    override suspend fun deleteMessage(id: Long) {
        _messages.value = _messages.value.filter { it.id != id }
    }

    override suspend fun clearMessages() {
        _messages.value = emptyList()
    }
}
