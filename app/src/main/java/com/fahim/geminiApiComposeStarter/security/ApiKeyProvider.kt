package com.fahim.geminiApiComposeStarter.security

/**
 * Interface providing secure on-demand access to the Gemini API key.
 */
interface ApiKeyProvider {
    /**
     * Decrypts and returns the API key in memory only when required.
     * Returns null if no valid key is configured.
     */
    fun getApiKey(): String?

    /**
     * Returns true if an encrypted API key is present in storage.
     */
    fun hasApiKey(): Boolean
}
