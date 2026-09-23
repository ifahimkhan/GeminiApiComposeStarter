package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyStorage
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val keyStorage: ApiKeyStorage,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val mutex = Mutex()
    private var keyPrepared = false
    private var model: GenerativeModel? = null

    // Must be called while holding mutex.
    private suspend fun prepareKey() {
        if (keyPrepared) return

        keyStorage.saveApiKey(BuildConfig.GEMINI_API_KEY)
        keyPrepared = true
    }

    // Encrypt the configured key when the app starts.
    suspend fun initialize(): Result<Unit> {
        return try {
            mutex.withLock {
                prepareKey()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "Could not prepare secure API-key storage."
                )
            )
        }
    }

    private suspend fun getModel(): GenerativeModel =
        mutex.withLock {
            model ?: run {
                // Retry preparation if startup initialization failed.
                prepareKey()

                // Decrypt only when constructing the model.
                GenerativeModel(
                    modelName = modelName,
                    apiKey = keyStorage.decryptApiKey()
                ).also {
                    model = it
                }
            }
        }

    override suspend fun generateText(prompt: String): Result<String> {
        return try {
            val response = getModel().generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }

            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(
                    IllegalStateException("Gemini returned an empty response.")
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val configuredKey = BuildConfig.GEMINI_API_KEY

            val safeMessage = (e.message ?: "No error details available")
                .let { message ->
                    if (configuredKey.isNotBlank()) {
                        message.replace(configuredKey, "[REDACTED]")
                    } else {
                        message
                    }
                }
                .take(400)

            Result.failure(
                IllegalStateException(
                    "${e.javaClass.simpleName}: $safeMessage"
                )
            )
        }
    }
}