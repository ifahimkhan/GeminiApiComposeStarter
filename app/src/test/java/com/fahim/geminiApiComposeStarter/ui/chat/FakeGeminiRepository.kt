package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository

class FakeGeminiRepository(
    var resultToReturn: Result<String> = Result.success("Default fake response"),
) : GeminiRepository {

    var lastPromptReceived: String? = null
        private set

    var callCount: Int = 0
        private set

    override suspend fun generateText(prompt: String): Result<String> {
        lastPromptReceived = prompt
        callCount++
        return resultToReturn
    }
}
