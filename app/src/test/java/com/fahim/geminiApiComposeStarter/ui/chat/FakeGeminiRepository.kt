package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository

class FakeGeminiRepository : GeminiRepository {
    var shouldFail = false
    var responseText = "Fake Gemini Response"

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldFail) {
            Result.failure(Exception("Fake error"))
        } else {
            Result.success(responseText)
        }
    }
}