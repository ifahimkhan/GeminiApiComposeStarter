package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String?,
    private val modelName: String = DEFAULT_MODEL,
    private val client: OkHttpClient = OkHttpClient(),
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        return try {
            val apiKey = apiKeyProvider()

            if (apiKey.isNullOrBlank()) {
                return Result.failure(
                    IllegalStateException("Gemini API key is unavailable")
                )
            }

            val requestJson = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().apply {
                                        put("text", prompt)
                                    }
                                )
                            )
                        }
                    )
                )
            }

            val requestBody = requestJson
                .toString()
                .toRequestBody(
                    "application/json".toMediaType()
                )

            val request = Request.Builder()
                .url(
                    "https://generativelanguage.googleapis.com/" +
                            "v1beta/models/$modelName:generateContent"
                )
                .addHeader(
                    "x-goog-api-key",
                    apiKey
                )
                .post(requestBody)
                .build()

            val response = withContext(Dispatchers.IO) {
                client
                    .newCall(request)
                    .execute()
            }

            response.use { httpResponse ->

                val responseBody =
                    httpResponse.body?.string().orEmpty()

                if (!httpResponse.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "Gemini request failed: " +
                                    "${httpResponse.code} $responseBody"
                        )
                    )
                }

                val root = JSONObject(responseBody)

                val candidates =
                    root.optJSONArray("candidates")

                val firstCandidate =
                    candidates?.optJSONObject(0)

                val content =
                    firstCandidate
                        ?.optJSONObject("content")

                val parts =
                    content?.optJSONArray("parts")

                val text =
                    parts
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.takeIf {
                            it.isNotBlank()
                        }

                if (text != null) {
                    Result.success(text)
                } else {
                    Result.failure(
                        IllegalStateException(
                            "Empty response from Gemini"
                        )
                    )
                }
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "Something went wrong"
                )
            )
        }
    }
}