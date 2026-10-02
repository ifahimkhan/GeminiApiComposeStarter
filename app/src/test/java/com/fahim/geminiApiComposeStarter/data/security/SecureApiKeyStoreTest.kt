package com.fahim.geminiApiComposeStarter.data.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SecureApiKeyStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile("keys.preferences_pb") }
    }

    /** Stand-in for the Keystore (unavailable on the JVM): a reversible XOR, not real crypto. */
    private class FakeCipher : SecretCipher {
        var encryptCalls = 0
        var broken = false
        override fun encrypt(plaintext: ByteArray): ByteArray {
            encryptCalls++
            return plaintext.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }
        override fun decrypt(blob: ByteArray): ByteArray {
            check(!broken) { "key invalidated" }
            return blob.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun roundTrip_neverPersistsPlaintext() = runBlocking {
        val store = SecureApiKeyStore(dataStore, FakeCipher(), buildTimeKey = "AIza-secret")

        store.provision()

        val persisted = dataStore.data.first().asMap().values.joinToString()
        assertFalse(persisted.contains("AIza-secret"))
        assertEquals("AIza-secret", store.decrypt())
    }

    @Test
    fun provision_isIdempotent_untilBuildKeyChanges() = runBlocking {
        val cipher = FakeCipher()
        SecureApiKeyStore(dataStore, cipher, "key-1").apply { provision(); provision() }
        assertEquals(1, cipher.encryptCalls)

        val rotated = SecureApiKeyStore(dataStore, cipher, "key-2")
        assertEquals("key-2", rotated.decrypt())
        assertEquals(2, cipher.encryptCalls)
    }

    @Test
    fun blankBuildKey_yieldsNull() = runBlocking {
        assertNull(SecureApiKeyStore(dataStore, FakeCipher(), "").decrypt())
    }

    @Test
    fun undecryptableBlob_isReprovisioned() = runBlocking {
        val cipher = FakeCipher()
        val store = SecureApiKeyStore(dataStore, cipher, "key")
        store.provision()

        // Simulate the Keystore key being lost for the old blob only.
        cipher.broken = true
        val failing = store.decrypt()
        cipher.broken = false

        assertNull(failing)
        assertEquals("key", store.decrypt())
    }
}
