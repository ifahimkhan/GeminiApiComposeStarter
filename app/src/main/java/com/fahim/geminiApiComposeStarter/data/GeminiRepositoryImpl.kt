package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.security.ApiKeyVault
import com.fahim.geminiApiComposeStarter.security.MissingCredentialException
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException

/** Keeps the starter repository contract. The SDK receives plaintext only at model creation. */
class GeminiRepositoryImpl(private val vault: ApiKeyVault, private val modelName: String) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> = generateText(prompt, modelName)
    override suspend fun generateText(prompt: String, modelId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val text = withTimeout(60_000) {
                val model = vault.withCredential { GenerativeModel(modelName = modelId.ifBlank { modelName }, apiKey = it) }
                model.generateContent(prompt).text?.takeIf { it.isNotBlank() }
            }
            if (text == null) Result.failure(Exception("No text was returned. Try rephrasing your question.")) else Result.success(text)
        } catch (_: TimeoutCancellationException) {
            Result.failure(Exception("The response took too long. Please try again."))
        } catch (e: CancellationException) { throw e
        } catch (e: MissingCredentialException) { Result.failure(e)
        } catch (_: IOException) { Result.failure(Exception("Could not connect. Check your internet connection and retry."))
        } catch (_: Exception) {
            // Do not expose raw SDK errors, request URLs, prompts or credentials in logs or UI.
            Result.failure(Exception("Gemini could not answer. Check the configured key, model and quota, then retry."))
        }
    }
}
