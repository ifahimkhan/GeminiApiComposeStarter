package com.fahim.geminiApiComposeStarter.fakes

import com.fahim.geminiApiComposeStarter.data.GeminiRepository

/** Scriptable fake so ViewModel tests don't hit the real Gemini API. */
class FakeGeminiRepository : GeminiRepository {
    var nextResult: Result<String> = Result.success("Fake response")
    var lastPrompt: String? = null

    override suspend fun generateText(prompt: String): Result<String> {
        lastPrompt = prompt
        return nextResult
    }
}
