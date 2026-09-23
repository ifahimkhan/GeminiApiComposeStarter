package com.fahim.geminiApiComposeStarter.data

class FakeGeminiRepository(
    var simulatedResult: Result<String> = Result.success("Fake Gemini response"),
) : GeminiRepository {

    val receivedPrompts = mutableListOf<String>()

    override suspend fun generateText(prompt: String): Result<String> {
        receivedPrompts.add(prompt)
        return simulatedResult
    }
}
