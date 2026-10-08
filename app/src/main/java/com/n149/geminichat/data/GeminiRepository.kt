package com.n149.geminichat.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that bridges the app and the Gemini API.
 *
 * The [GenerativeModel] is created lazily using the key decrypted
 * in-memory from Keystore. The plaintext key is NEVER stored as a
 * class field or logged.
 *
 * A personalised system instruction is injected on each call so
 * Gemini adapts its answers to the user's topic and feedback history.
 *
 * Interface allows a fake implementation in unit tests.
 */
interface GeminiRepositoryInterface {
    suspend fun sendMessage(
        history: List<MessageEntity>,
        newMessage: String,
        systemPrompt: String = ""
    ): String
}

@Singleton
class GeminiRepository @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val buildConfigApiKey: String
) : GeminiRepositoryInterface {

    /** Decrypt and build the GenerativeModel. Key is never stored as a field. */
    private fun buildModel(systemInstruction: String): GenerativeModel {
        val storedKey = keystoreManager.decryptApiKey()
        val apiKey = if (buildConfigApiKey.isNotBlank() && storedKey != buildConfigApiKey) {
            buildConfigApiKey.also { keystoreManager.encryptAndStore(it) }
        } else {
            storedKey ?: buildConfigApiKey
        }

        return GenerativeModel(
            modelName = "gemini-3.5-flash-lite",
            apiKey = apiKey,
            systemInstruction = content { text(systemInstruction) },
            generationConfig = generationConfig {
                temperature = 0.75f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 2048
            }
        )
    }

    /**
     * Sends [newMessage] to Gemini with the full [history] as multi-turn context
     * and a personalised [systemPrompt] that steers tone, length and topic focus.
     * Runs on [Dispatchers.IO] – never blocks the main thread.
     */
    override suspend fun sendMessage(
        history: List<MessageEntity>,
        newMessage: String,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val model = buildModel(systemPrompt.ifBlank {
            "You are Gemini, a helpful, honest and harmless AI assistant."
        })

        val chat = model.startChat(
            history = history
                .filter { !it.isLoading && it.errorMessage == null }
                .map { msg -> content(role = msg.role) { text(msg.content) } }
        )

        val response = chat.sendMessage(newMessage)
        response.text ?: throw IllegalStateException("Empty response from Gemini")
    }
}
