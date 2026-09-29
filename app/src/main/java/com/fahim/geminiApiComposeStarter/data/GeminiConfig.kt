package com.fahim.geminiApiComposeStarter.data

/**
 * Centralized configuration for the Google Gemini API.
 *
 * Model: gemini-3.8-flash — GA since September 2, 2026.
 *
 * SDK Note:
 * This project uses com.google.ai.client.generativeai:0.9.0 (the last published version
 * of the legacy Android Generative AI SDK). This SDK is deprecated in favour of
 * com.google.genai:google-genai-kotlin-android, however migrating to that SDK from an
 * Android mobile app that directly supplies an API key is not currently supported
 * (the new SDK throws IllegalStateException when an API key is provided on Android).
 * The recommended migration path for mobile apps is Firebase AI Logic, which requires
 * a Firebase project — outside the scope of this lab assignment.
 * The legacy SDK remains functional: model names are passed as plain strings to the
 * v1beta generateContent REST endpoint which still accepts gemini-3.8-flash.
 */
object GeminiConfig {
    const val MODEL_NAME = "gemini-3.8-flash"
}
