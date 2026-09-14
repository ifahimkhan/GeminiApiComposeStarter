package com.fahim.geminiApiComposeStarter.fakes

import com.fahim.geminiApiComposeStarter.data.GeminiRepository

class FakeGeminiRepository : GeminiRepository {
    var response: Result<String> = Result.success("fake response")
    var lastPrompt: String? = null

    override suspend fun generateText(prompt: String): Result<String> {
        lastPrompt = prompt
        return response
    }
}
