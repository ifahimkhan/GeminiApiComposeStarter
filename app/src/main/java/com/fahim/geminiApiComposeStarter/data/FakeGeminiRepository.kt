package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.delay

/**
 * Fake implementation of [GeminiRepository] for offline unit testing and Compose UI testing.
 *
 * Does not make any live network requests or require an API key.
 */
class FakeGeminiRepository(
    var simulatedDelayMs: Long = 0L,
    var shouldFail: Boolean = false,
    var failureException: Exception = RuntimeException("Simulated Gemini API error"),
    var customResponse: String? = null,
) : GeminiRepository {

    var lastPromptReceived: String? = null
        private set

    var callCount: Int = 0
        private set

    override suspend fun generateText(prompt: String): Result<String> {
        lastPromptReceived = prompt
        callCount++

        if (simulatedDelayMs > 0) {
            delay(simulatedDelayMs)
        }

        if (shouldFail) {
            return Result.failure(failureException)
        }

        val responseText = customResponse ?: "Echo from Fake Gemini: $prompt"
        return Result.success(responseText)
    }

    fun reset() {
        lastPromptReceived = null
        callCount = 0
        shouldFail = false
        customResponse = null
    }
}
