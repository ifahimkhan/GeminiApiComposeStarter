package com.fahim.geminiApiComposeStarter.data

import java.io.IOException

/**
 * Sealed hierarchy of typed domain exceptions for Gemini operations (Phase 5b).
 * Each variant implements [GeminiException] while preserving standard exception subtypes
 * (e.g. [IOException], [IllegalStateException], [IllegalArgumentException]) for backwards
 * compatibility with general catch blocks.
 */
sealed interface GeminiException {
    val message: String?
    val cause: Throwable?

    /** Network connectivity issues (timeout, DNS resolution, connection drop) -> Retryable */
    class Network(
        message: String = "Network connection error. Please check your internet connection and try again.",
        cause: Throwable? = null,
    ) : IOException(message, cause), GeminiException

    /** 503 Service Unavailable / Transient high demand -> Retryable */
    class Overloaded(
        message: String = "The model is currently experiencing high demand. Please try again in a moment.",
        cause: Throwable? = null,
    ) : IllegalStateException(message, cause), GeminiException

    /** 429 Rate limit or quota exhaustion -> Retryable */
    class QuotaExceeded(
        message: String = "Rate limit or quota exceeded. Please wait a moment and try again.",
        cause: Throwable? = null,
    ) : IllegalStateException(message, cause), GeminiException

    /** 401 or invalid API key credentials -> Non-retryable (same key will always fail) */
    class Authentication(
        message: String = "Invalid API key credentials. Please verify your GEMINI_API_KEY in local.properties.",
        cause: Throwable? = null,
    ) : IllegalArgumentException(message, cause), GeminiException

    /** 403 or permission denied -> Non-retryable */
    class PermissionDenied(
        message: String = "Authentication or permission denied for Gemini API.",
        cause: Throwable? = null,
    ) : SecurityException(message, cause), GeminiException

    /** 404 Model Not Found / Unsupported model name -> Non-retryable */
    class ModelNotFound(
        message: String = "The requested model is currently unavailable.",
        cause: Throwable? = null,
    ) : IllegalStateException(message, cause), GeminiException

    /** Content blocked by safety/policy filters -> Non-retryable */
    class ContentBlocked(
        message: String = "Response was blocked due to safety or policy settings.",
        cause: Throwable? = null,
    ) : IllegalStateException(message, cause), GeminiException

    /** 400 or invalid request parameters -> Non-retryable */
    class BadRequest(
        message: String = "Invalid request format or parameters.",
        cause: Throwable? = null,
    ) : IllegalArgumentException(message, cause), GeminiException

    /**
     * Unclassified / unknown exceptions.
     * Decision & Documentation: Non-retryable by default. Retrying unknown errors
     * risks uncontrolled loops, wasting quota, or repeating invalid requests.
     */
    class Unknown(
        message: String = "An unexpected error occurred while communicating with Gemini.",
        cause: Throwable? = null,
    ) : Exception(message, cause), GeminiException
}
