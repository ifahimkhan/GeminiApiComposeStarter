package com.fahim.geminiApiComposeStarter.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device/emulator because the Android Keystore is not available on the JVM. */
@RunWith(AndroidJUnit4::class)
class KeystoreCipherTest {

    private val cipher = KeystoreCipher(keyAlias = "test_alias_gemini")

    @After
    fun tearDown() = cipher.deleteKey()

    @Test
    fun encryptThenDecrypt_returnsOriginal() {
        val secret = "dummy-value-for-test".toByteArray()
        val payload = cipher.encrypt(secret)

        assertEquals(12, payload.iv.size) // GCM standard IV length
        assertFalse(payload.ciphertext.contentEquals(secret))
        assertArrayEquals(secret, cipher.decrypt(payload))
    }

    @Test
    fun eachEncryption_usesAFreshIv() {
        val a = cipher.encrypt("same".toByteArray())
        val b = cipher.encrypt("same".toByteArray())
        assertFalse(a.iv.contentEquals(b.iv))
        assertFalse(a.ciphertext.contentEquals(b.ciphertext))
    }
}
