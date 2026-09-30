package com.fahim.geminiApiComposeStarter.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for [SecureApiKeyProvider.sha256HexOf], the fingerprint function
 * that detects API key rotation (§2.1).
 *
 * The full seeding + decryption path is covered by instrumented tests in
 * [SecureApiKeyProviderTest] (requires AndroidKeyStore + DataStore).
 */
class ApiKeyProviderUnitTest {

    @Test
    fun sha256Hex_deterministicForSameInput() {
        val fp1 = SecureApiKeyProvider.sha256HexOf("FAKE_TEST_KEY_NOT_REAL_12345")
        val fp2 = SecureApiKeyProvider.sha256HexOf("FAKE_TEST_KEY_NOT_REAL_12345")
        assertEquals("Same input must produce same fingerprint", fp1, fp2)
    }

    @Test
    fun sha256Hex_differentForDifferentInputs() {
        val fp1 = SecureApiKeyProvider.sha256HexOf("FAKE_OLD_KEY_NOT_REAL_00000000000000000000")
        val fp2 = SecureApiKeyProvider.sha256HexOf("FAKE_NEW_KEY_NOT_REAL_11111111111111111111")
        assertNotEquals("Different keys must produce different fingerprints", fp1, fp2)
    }

    @Test
    fun sha256Hex_outputIs64LowercaseHexChars() {
        val fp = SecureApiKeyProvider.sha256HexOf("anyKey")
        assertEquals("SHA-256 hex output must be 64 characters", 64, fp.length)
        assertTrue("SHA-256 hex output must be lowercase hex", fp.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun sha256Hex_emptyStringProducesKnownDigest() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertEquals(expected, SecureApiKeyProvider.sha256HexOf(""))
    }
}
