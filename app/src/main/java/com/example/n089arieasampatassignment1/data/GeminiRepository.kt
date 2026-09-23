package com.example.n089arieasampatassignment1.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiRepository(
    private val apiKey: String
) {

    suspend fun generateResponse(
        prompt: String
    ): String = withContext(Dispatchers.IO) {

        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"
        )

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "POST"

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.setRequestProperty(
                "X-goog-api-key",
                apiKey
            )

            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            val requestBody = JSONObject().apply {

                put(
                    "contents",
                    org.json.JSONArray().apply {

                        put(
                            JSONObject().apply {

                                put(
                                    "parts",
                                    org.json.JSONArray().apply {

                                        put(
                                            JSONObject().apply {
                                                put(
                                                    "text",
                                                    prompt
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }

            connection.outputStream.use { outputStream ->

                outputStream.write(
                    requestBody
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val responseCode =
                connection.responseCode

            val responseText =
                if (responseCode in 200..299) {

                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                } else {

                    connection.errorStream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: "Unknown API error"
                }

            if (responseCode !in 200..299) {

                throw Exception(
                    "Gemini API Error $responseCode: $responseText"
                )
            }

            val json =
                JSONObject(responseText)

            val candidates =
                json.optJSONArray("candidates")

            if (
                candidates == null ||
                candidates.length() == 0
            ) {
                return@withContext "Gemini returned no response."
            }

            val firstCandidate =
                candidates.getJSONObject(0)

            val content =
                firstCandidate.optJSONObject("content")

            val parts =
                content?.optJSONArray("parts")

            if (
                parts == null ||
                parts.length() == 0
            ) {
                return@withContext "Gemini returned no text."
            }

            val text =
                parts.getJSONObject(0)
                    .optString("text")

            if (text.isBlank()) {
                "Gemini returned an empty response."
            } else {
                text
            }

        } finally {

            connection.disconnect()
        }
    }
}
