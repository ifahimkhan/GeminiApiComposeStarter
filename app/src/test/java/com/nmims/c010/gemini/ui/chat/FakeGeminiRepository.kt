package com.nmims.c010.gemini.ui.chat

import com.nmims.c010.gemini.data.ChatMessage
import com.nmims.c010.gemini.data.GeminiRepository

class FakeGeminiRepository(
    private var result: Result<String> = Result.success("Hello from Gemini")
) : GeminiRepository {

    var lastPrompt: String? = null

    fun failWith(t: Throwable) {
        result = Result.failure(t)
    }

    override suspend fun send(prompt: String, history: List<ChatMessage>): Result<String> {
        lastPrompt = prompt
        return result
    }
}
