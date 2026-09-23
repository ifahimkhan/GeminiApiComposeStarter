package com.fahim.geminiApiComposeStarter.data

class FakeGeminiRepository(
    var resultToReturn: Result<String> = Result.success("Default fake response"),
) : GeminiRepository {

    var callCount: Int = 0
    var lastPromptReceived: String? = null

    override suspend fun generateText(prompt: String): Result<String> {
        callCount++
        lastPromptReceived = prompt
        return resultToReturn
    }
}
