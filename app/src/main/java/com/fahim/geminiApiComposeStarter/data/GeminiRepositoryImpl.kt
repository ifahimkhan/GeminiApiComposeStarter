
package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

private const val TAG = "GeminiRepository"

// Gemini model
private const val DEFAULT_MODEL = "gemini-3.6-flash"

private const val BASE_URL =
    "https://generativelanguage.googleapis.com/v1beta/models"

/**
 * Calls the Gemini REST API directly using HttpURLConnection.
 */
class GeminiRepositoryImpl(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    /**
     * Generates text using Gemini.
     */
    override suspend fun generateText(
        prompt: String
    ): Result<String> =
        withContext(Dispatchers.IO) {

            try {
                Result.success(
                    callGeminiWithRetry(prompt)
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: UnknownHostException) {

                Log.e(
                    TAG,
                    "No network connection",
                    e
                )

                Result.failure(
                    IOException(
                        "No internet connection. Check your network and try again."
                    )
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "generateContent failed",
                    e
                )

                Result.failure(e)
            }
        }

    /**
     * Retries temporary Gemini errors.
     */
    private suspend fun callGeminiWithRetry(
        prompt: String
    ): String {

        var lastException: Exception? = null

        repeat(3) { attempt ->

            try {

                return callGemini(prompt)

            } catch (e: IOException) {

                lastException = e

                val errorMessage =
                    e.message.orEmpty()

                val isTemporaryError =
                    errorMessage.contains("Error 503") ||
                            errorMessage.contains("Error 429")

                if (
                    !isTemporaryError ||
                    attempt == 2
                ) {
                    throw e
                }

                val delayMillis =
                    2000L * (attempt + 1)

                Log.w(
                    TAG,
                    "Temporary Gemini error. " +
                            "Retrying in ${delayMillis}ms..."
                )

                delay(delayMillis)
            }
        }

        throw lastException
            ?: IOException("Gemini request failed")
    }

    /**
     * Sends a prompt to Gemini.
     */
    private fun callGemini(
        prompt: String
    ): String {

        val requestBody = JSONObject().put(
            "contents",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put(
                                "text",
                                prompt
                            )
                        )
                    )
            )
        )

        val url = URL(
            "$BASE_URL/$modelName:generateContent"
        )

        val connection =
            (url.openConnection() as HttpURLConnection).apply {

                requestMethod = "POST"

                connectTimeout = 30_000

                readTimeout = 120_000

                doOutput = true

                setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                setRequestProperty(
                    "x-goog-api-key",
                    apiKey
                )
            }

        try {

            // Send request
            connection.outputStream.use { output ->

                output.write(
                    requestBody
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val responseCode =
                connection.responseCode

            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseBody =
                stream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()

            // Handle API errors
            if (responseCode !in 200..299) {

                Log.e(
                    TAG,
                    "HTTP $responseCode: $responseBody"
                )

                throw IOException(
                    buildErrorMessage(
                        responseCode,
                        responseBody
                    )
                )
            }

            // Extract Gemini response text
            return extractText(responseBody)

        } finally {

            connection.disconnect()
        }
    }

    /**
     * Extracts text from:
     *
     * candidates[0].content.parts[*].text
     */
    private fun extractText(
        responseBody: String
    ): String {

        val root =
            JSONObject(responseBody)

        val candidates =
            root.optJSONArray("candidates")

        if (
            candidates == null ||
            candidates.length() == 0
        ) {

            val blockReason =
                root.optJSONObject("promptFeedback")
                    ?.optString(
                        "blockReason",
                        ""
                    )
                    .orEmpty()

            throw IllegalStateException(

                if (blockReason.isNotEmpty()) {

                    "Gemini blocked this prompt " +
                            "($blockReason). Try rephrasing it."

                } else {

                    "Empty response from Gemini"
                }
            )
        }

        val firstCandidate =
            candidates.getJSONObject(0)

        val parts =
            firstCandidate
                .optJSONObject("content")
                ?.optJSONArray("parts")

        val text =
            StringBuilder()

        if (parts != null) {

            for (i in 0 until parts.length()) {

                val part =
                    parts.getJSONObject(i)

                // Skip hidden thinking text
                if (
                    part.optBoolean(
                        "thought",
                        false
                    )
                ) {
                    continue
                }

                text.append(
                    part.optString(
                        "text",
                        ""
                    )
                )
            }
        }

        val answer =
            text.toString().trim()

        if (answer.isEmpty()) {

            val reason =
                firstCandidate.optString(
                    "finishReason",
                    "UNKNOWN"
                )

            throw IllegalStateException(
                "Gemini returned no text " +
                        "(reason: $reason). Try again."
            )
        }

        return answer
    }

    /**
     * Converts HTTP errors into readable messages.
     */
    private fun buildErrorMessage(
        code: Int,
        responseBody: String
    ): String {

        val apiMessage =
            try {

                JSONObject(responseBody)
                    .optJSONObject("error")
                    ?.optString(
                        "message",
                        ""
                    )
                    .orEmpty()

            } catch (e: Exception) {

                ""
            }

        val hint =
            when (code) {

                400 ->
                    "Bad request. Check your Gemini API request."

                401, 403 ->
                    "Your API key was rejected. " +
                            "Check your Gemini API key."

                404 ->
                    "Model \"$modelName\" was not found. " +
                            "Check the model name and API access."

                429 ->
                    "Too many requests. " +
                            "Wait a minute and try again."

                in 500..599 ->
                    "Gemini server problem. " +
                            "Please try again shortly."

                else ->
                    "Request failed."
            }

        return if (apiMessage.isNotBlank()) {

            "Error $code: $apiMessage"

        } else {

            "Error $code: $hint"
        }
    }
}