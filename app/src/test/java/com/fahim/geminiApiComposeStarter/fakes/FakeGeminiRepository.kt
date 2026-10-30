package com.fahim.geminiApiComposeStarter.fakes

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.delay

class FakeGeminiRepository : GeminiRepository {
    var responseResult: Result<String> = Result.success("Fake Gemini response")
    var isConfigured: Boolean = true
    var responseDelayMs: Long = 0L

    override suspend fun isApiKeyConfigured(): Boolean = isConfigured

    override suspend fun generateText(prompt: String): Result<String> {
        if (responseDelayMs > 0) {
            delay(responseDelayMs)
        }
        return responseResult
    }
}
