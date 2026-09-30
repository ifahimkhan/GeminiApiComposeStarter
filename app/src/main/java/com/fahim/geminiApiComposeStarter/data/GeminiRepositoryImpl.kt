package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyManager: SecureApiKeyManager,
    private val modelName: String = DEFAULT_MODEL,
    private val modelFactory: ((modelName: String, apiKey: String) -> GenerativeModel)? = null,
    private val chatSessionFactory: ((GenerativeModel) -> Chat)? = null,
) : GeminiRepository {

    @Volatile
    private var model: GenerativeModel? = null

    @Volatile
    private var activeChatSession: Chat? = null

    private fun getOrCreateModel(): GenerativeModel {
        model?.let { return it }
        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("Gemini API key is not configured or invalid.")
        }
        return synchronized(this) {
            model ?: (modelFactory?.invoke(modelName, apiKey)
                ?: GenerativeModel(modelName = modelName, apiKey = apiKey)).also {
                model = it
            }
        }
    }

    private fun getOrCreateChatSession(): Chat {
        activeChatSession?.let { return it }
        val activeModel = getOrCreateModel()
        return synchronized(this) {
            activeChatSession ?: (chatSessionFactory?.invoke(activeModel)
                ?: activeModel.startChat()).also {
                activeChatSession = it
            }
        }
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val activeModel = getOrCreateModel()
        val response = activeModel.generateContent(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Sanitize logging to prevent URLs with query keys or credentials from leaking to Logcat
        val errorType = e.javaClass.simpleName
        Log.e(TAG, "generateContent failed with error type: $errorType")

        val safeMessage = sanitizeErrorMessage(e.message)
        Result.failure(Exception(safeMessage))
    }

    override fun sendMessageStream(prompt: String): Flow<String> = flow {
        val chat = getOrCreateChatSession()
        chat.sendMessageStream(prompt).collect { response ->
            val text = response.text
            if (!text.isNullOrEmpty()) {
                emit(text)
            }
        }
    }.catch { e ->
        if (e is CancellationException) {
            throw e
        }
        val errorType = e.javaClass.simpleName
        Log.e(TAG, "sendMessageStream failed with error type: $errorType")

        val safeMessage = sanitizeErrorMessage(e.message)
        throw Exception(safeMessage)
    }

    override fun resetChatSession() {
        synchronized(this) {
            activeChatSession = null
        }
    }

    private fun sanitizeErrorMessage(rawMessage: String?): String {
        if (rawMessage.isNullOrBlank()) {
            return "An error occurred while generating content."
        }
        if (rawMessage.contains("key=", ignoreCase = true) ||
            rawMessage.contains("AIza", ignoreCase = true) ||
            rawMessage.contains("api_key", ignoreCase = true) ||
            rawMessage.contains("authorization", ignoreCase = true)
        ) {
            return "Failed to generate content due to an API request error."
        }
        return rawMessage
    }
}
