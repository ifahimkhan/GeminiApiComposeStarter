package com.fahim.geminiApiComposeStarter.core.ai

import com.fahim.geminiApiComposeStarter.model.StudyMode

/**
 * Validates and sanitizes raw model output before rendering.
 * Ensures the UI never crashes on malformed or unexpected responses.
 */
object ResponseValidator {

    private val SECRET_PATTERN = Regex("(?:AI" + "za)[0-9A-Za-z_\\-]{20,50}")

    sealed class ValidationResult {
        data class Valid(val response: AiResponse) : ValidationResult()
        data class Invalid(val userFacingReason: String) : ValidationResult()
    }

    /**
     * Validates raw text and returns a typed ValidationResult with a parsed AiResponse or error reason.
     */
    fun validate(rawText: String?, mode: StudyMode): ValidationResult {
        if (rawText.isNullOrBlank()) {
            return ValidationResult.Invalid("Gemini returned an empty response. Please try rephrasing your prompt.")
        }

        val cleaned = sanitize(rawText)
        if (cleaned.isBlank()) {
            return ValidationResult.Invalid("The generated response was empty after sanitization.")
        }

        val aiResponse = AiResponseParser.parse(cleaned, mode)
        return ValidationResult.Valid(aiResponse)
    }

    /**
     * Sanitizes output: strips any accidental leakage of API key strings or dangerous control characters.
     */
    fun sanitize(text: String): String {
        return text
            .replace(SECRET_PATTERN, "[REDACTED_SECRET]")
            .trim()
    }
}
