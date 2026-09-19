package com.fahim.geminiApiComposeStarter.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Runs on a device/emulator because the Android Keystore is not available in JVM unit tests. */
class KeystoreCipherTest {

    private val cipher = KeystoreCipher(keyAlias = "test_alias_gemini")

    @Test
    fun encryptThenDecrypt_returnsOriginalText() {
        val secret = "not-a-real-key-1234"
        assertEquals(secret, cipher.decrypt(cipher.encrypt(secret)))
    }

    @Test
    fun ciphertext_doesNotContainPlainText() {
        val secret = "not-a-real-key-1234"
        assertFalse(cipher.encrypt(secret).contains(secret))
    }

    @Test
    fun encryptingTwice_givesDifferentPayloads_becauseIvIsRandom() {
        val secret = "not-a-real-key-1234"
        assertNotEquals(cipher.encrypt(secret), cipher.encrypt(secret))
    }
}
