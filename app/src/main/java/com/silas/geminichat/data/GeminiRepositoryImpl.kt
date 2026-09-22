package com.silas.geminichat.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.silas.geminichat.data.secure.ApiKeyManager
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"

/**
 * Talks to the Gemini API. The plaintext API key is decrypted from Keystore-
 * backed ciphertext only at the moment the [GenerativeModel] is created, and is
 * never logged, displayed, or persisted in plaintext.
 *
 * @author Silas · n091 (MBA Tech CE, E1)
 */
class GeminiRepositoryImpl(
    private val apiKeyManager: ApiKeyManager,
    private val rawApiKey: String?,
    private val modelName: String = GeminiRepository.DEFAULT_MODEL,
) : GeminiRepository {

    @Volatile
    private var model: GenerativeModel? = null

    private suspend fun model(): GenerativeModel {
        model?.let { return it }
        val apiKey = apiKeyManager.decryptApiKey(rawApiKey)
            ?: throw IllegalStateException("Gemini API key is not available")
        return GenerativeModel(modelName = modelName, apiKey = apiKey).also { model = it }
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = model().generateContent(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(e)
    }
}
