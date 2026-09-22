package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String?,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
        val apiKey = apiKeyProvider()?.takeIf { it.isNotBlank() }
            ?: run {
                Log.e(TAG, "Gemini key provider returned no usable key")
                return@withContext Result.failure(IllegalStateException("Gemini is not configured"))
            }
        val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
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
            // Keep diagnostic information useful without ever allowing a key to reach Logcat.
            Log.e(TAG, "Gemini request failed: ${e.javaClass.simpleName}: ${redactSecrets(e.message)}")
            Result.failure(e)
        }
    }

    private fun redactSecrets(message: String?): String = message.orEmpty()
        .replace(Regex("AIza[0-9A-Za-z_-]+"), "[redacted]")
        .replace(Regex("(?i)(key=)[^&\\s]+"), "\$1[redacted]")
        .take(500)
}
