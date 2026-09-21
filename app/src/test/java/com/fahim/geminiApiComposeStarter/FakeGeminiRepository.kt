package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.CompletableDeferred

class FakeGeminiRepository : GeminiRepository {

    var shouldReturnError = false
    var errorMessage = "Simulated API error"
    var cannedResponse = "This is a fake Gemini response"

    val generatedPrompts = mutableListOf<String>()

    // Optional deferred gate to test in-flight loading state precisely
    var responseDeferred: CompletableDeferred<Unit>? = null

    override suspend fun generateText(prompt: String): Result<String> {
        generatedPrompts.add(prompt)
        responseDeferred?.await()

        return if (shouldReturnError) {
            Result.failure(Exception(errorMessage))
        } else {
            Result.success(cannedResponse)
        }
    }
}
