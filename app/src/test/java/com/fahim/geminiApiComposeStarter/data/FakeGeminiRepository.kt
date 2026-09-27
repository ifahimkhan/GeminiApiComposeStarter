package com.fahim.geminiApiComposeStarter.data

/** Deterministic test double: never touches the network. */
class FakeGeminiRepository(
    private val response: String = "Fake Gemini response",
    private val shouldFail: Boolean = false,
    private val failureMessage: String = "Simulated failure",
) : GeminiRepository {

    var callCount: Int = 0
        private set

    var lastPrompt: String? = null
        private set

    override suspend fun generateText(prompt: String): Result<String> {
        callCount++
        lastPrompt = prompt
        return if (shouldFail) {
            Result.failure(IllegalStateException(failureMessage))
        } else {
            Result.success(response)
        }
    }
}
