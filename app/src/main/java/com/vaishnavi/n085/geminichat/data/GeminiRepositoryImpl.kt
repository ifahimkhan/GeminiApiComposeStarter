package com.vaishnavi.n085.geminichat.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

/**
 * Talks to the Gemini API. The key is resolved lazily via [apiKeyProvider] so it is
 * decrypted in memory only at the moment the GenerativeModel is built (see ApiKeyStore).
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        return try {
            val apiKey = apiKeyProvider()
            if (apiKey.isBlank()) {
                Result.failure(
                    IllegalStateException("GEMINI_API_KEY is missing. Add it to local.properties and rebuild.")
                )
            } else {
                val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
                val response = model.generateContent(prompt)
                val text = response.text?.takeIf { it.isNotBlank() }
                if (text != null) {
                    Result.success(text)
                } else {
                    Result.failure(IllegalStateException("Empty response from Gemini"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed", e)
            Result.failure(e)
        }
    }
}
