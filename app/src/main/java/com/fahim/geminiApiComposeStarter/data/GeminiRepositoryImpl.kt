package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-1.5-flash"

/**
 * Implementation of GeminiRepository.
 * API key is decrypted in memory dynamically via apiKeyProvider only at the moment
 * GenerativeModel is instantiated. Plaintext keys are never logged, leaked, or serialized.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String?,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    /** Convenience constructor for testing or direct string usage */
    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this({ apiKey }, modelName)

    override suspend fun generateText(prompt: String, systemInstruction: String?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = apiKeyProvider()?.takeIf { it.isNotBlank() }
                    ?: return@withContext Result.failure(
                        IllegalStateException("Gemini API key is not available or could not be decrypted.")
                    )

                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey,
                    systemInstruction = systemInstruction?.takeIf { it.isNotBlank() }?.let { instruction ->
                        content { text(instruction) }
                    }
                )

                val response = model.generateContent(prompt)
                val text = response.text?.takeIf { it.isNotBlank() }
                if (text != null) {
                    Result.success(text)
                } else {
                    Result.failure(IllegalStateException("Empty response from Gemini"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateContent failed: ${e.javaClass.simpleName}: ${e.message}")
                Result.failure(e)
            }
        }
}
