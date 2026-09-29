package com.fahim.geminiApiComposeStarter.data

/**
 * Centralized configuration for the Google Gemini API.
 *
 * Primary Model: gemini-3.8-flash
 * Fallback Model: gemini-2.5-flash
 *
 * SDK Compatibility:
 * Compatible with com.google.ai.client.generativeai:generativeai:0.9.0.
 * The SDK forwards the model name parameter directly to the v1beta generateContent endpoint.
 *
 * Rationale:
 * According to Google AI for Developers official documentation (ai.google.dev/gemini-api/docs/models),
 * older models such as gemini-1.5-flash and gemini-2.0-flash have been shut down/deprecated.
 * gemini-3.8-flash is the currently supported, high-speed, cost-efficient Flash-tier model recommended
 * for multi-turn conversational agents, while gemini-2.5-flash serves as a reliable fallback.
 */
object GeminiConfig {
    const val MODEL_NAME = "gemini-3.8-flash"
    const val FALLBACK_MODEL_NAME = "gemini-2.5-flash"
}
