package com.example.c002_dhruvil_assignment1.data

import android.content.Context
import com.example.c002_dhruvil_assignment1.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

interface GeminiRepository {

    suspend fun sendMessage(prompt: String): String

    fun sendMessageStream(prompt: String): Flow<String>
}

class GeminiRepositoryImpl(
    private val context: Context
) : GeminiRepository {

    private val apiKey: String
        get() = ApiKeyManager.getOrStoreApiKey(
            context,
            BuildConfig.GEMINI_API_KEY
        )

    private val client = HttpClient(Android)

    private val modelName = "gemini-3.6-flash"

    private fun validateApiKey(key: String) {
        require(key.isNotBlank()) {
            "GEMINI_API_KEY is missing in local.properties."
        }
    }

    override suspend fun sendMessage(prompt: String): String {
        validateApiKey(apiKey)

        return try {

            val response = client.post(
                "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
            ) {

                // Send Gemini API key securely through the HTTP header
                header("x-goog-api-key", apiKey)

                contentType(ContentType.Application.Json)

                setBody(
                    """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": ${JSONObject.quote(prompt)}
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent()
                )
            }

            // Read Google's complete response
            val responseText = response.bodyAsText()

            // Check HTTP status before trying to read candidates
            if (response.status.value !in 200..299) {
                throw Exception(
                    "Gemini API error (${response.status.value}): $responseText"
                )
            }

            val json = JSONObject(responseText)

            // Make sure Gemini actually returned candidates
            if (!json.has("candidates")) {
                throw Exception(
                    "Gemini returned an unexpected response: $responseText"
                )
            }

            val candidates = json.getJSONArray("candidates")

            if (candidates.length() == 0) {
                throw Exception(
                    "Gemini returned no response candidates."
                )
            }

            // Extract Gemini's text response
            candidates
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

        } catch (e: Exception) {

            throw Exception(
                "Gemini request failed: ${e.message ?: "Unknown error"}"
            )
        }
    }

    override fun sendMessageStream(prompt: String): Flow<String> = flow {

        try {
            val result = sendMessage(prompt)

            emit(result)

        } catch (e: Exception) {

            throw e
        }
    }
}