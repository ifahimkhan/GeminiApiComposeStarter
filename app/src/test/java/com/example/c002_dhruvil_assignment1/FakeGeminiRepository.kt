package com.example.c002_dhruvil_assignment1

import com.example.c002_dhruvil_assignment1.data.GeminiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeGeminiRepository(
    var shouldReturnError: Boolean = false,
    var mockResponse: String = "Fake Gemini Response"
) : GeminiRepository {

    override suspend fun sendMessage(prompt: String): String {
        if (shouldReturnError) {
            throw Exception("API Error")
        }
        return mockResponse
    }

    override fun sendMessageStream(prompt: String): Flow<String> = flow {
        if (shouldReturnError) {
            throw Exception("API Error")
        }
        emit(mockResponse)
    }
}
