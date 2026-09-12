package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    fun generateTextStream(
        prompt: String,
        history: List<ConversationMessage>,
    ): Flow<Result<String>>
}
