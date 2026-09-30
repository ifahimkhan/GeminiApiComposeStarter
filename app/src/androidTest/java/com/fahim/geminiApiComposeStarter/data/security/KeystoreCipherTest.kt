package com.fahim.geminiApiComposeStarter.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/** Runs against the real Android Keystore, which is unavailable in JVM unit tests. */
@RunWith(AndroidJUnit4::class)
class KeystoreCipherTest {

    private val alias = "test_alias_${System.nanoTime()}"
    private val cipher = KeystoreCipher(alias)

    @Test
    fun roundTrip_restoresPlaintext() {
        val secret = "AIza-not-a-real-key".toByteArray()

        val blob = cipher.encrypt(secret)

        assertFalse(blob.contentEquals(secret))
        assertArrayEquals(secret, cipher.decrypt(blob))
    }

    @Test
    fun sameInput_encryptsDifferentlyEachTime() {
        val secret = "same".toByteArray()

        // Fresh random IV per call, so ciphertexts must differ.
        assertFalse(cipher.encrypt(secret).contentEquals(cipher.encrypt(secret)))
    }

    @Test(expected = Exception::class)
    fun tamperedCiphertext_isRejected() {
        val blob = cipher.encrypt("secret".toByteArray())
        blob[blob.lastIndex] = (blob.last().toInt() xor 1).toByte()

        cipher.decrypt(blob) // GCM authentication tag check fails.
    }

    @org.junit.After
    fun tearDown() {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }
}
