package com.nmims.c010.gemini.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nmims.c010.gemini.security.ApiKeyStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface GeminiRepository {
    suspend fun send(prompt: String, history: List<ChatMessage>): Result<String>
}

class GeminiRepositoryImpl(
    private val apiKeyStore: ApiKeyStore,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : GeminiRepository {

    // built lazily so the decrypted key stays in memory for as little time as possible
    private suspend fun model(): GenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKeyStore.apiKey()
    )

    override suspend fun send(prompt: String, history: List<ChatMessage>): Result<String> =
        withContext(io) {
            runCatching {
                val past = history
                    .dropLast(1)          // last item is the prompt we are about to send
                    .map { msg ->
                        content(role = if (msg.author == Author.USER) "user" else "model") {
                            text(msg.text)
                        }
                    }
                val chat = model().startChat(history = past)
                chat.sendMessage(prompt).text ?: error("Gemini returned an empty response")
            }
        }
}
