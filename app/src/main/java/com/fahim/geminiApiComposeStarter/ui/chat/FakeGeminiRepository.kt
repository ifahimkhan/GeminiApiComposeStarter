package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository

class FakeGeminiRepository : GeminiRepository {
    var result: Result<String> = Result.success("Fake response")
    var lastPrompt: String? = null

    override suspend fun generateText(prompt: String): Result<String> {
        lastPrompt = prompt
        return result
    }
}