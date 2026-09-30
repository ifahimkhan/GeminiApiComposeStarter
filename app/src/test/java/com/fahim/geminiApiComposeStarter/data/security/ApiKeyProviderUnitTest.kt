package com.fahim.geminiApiComposeStarter.data.security

import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

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

    @org.junit.Rule
    @JvmField
    val temporaryFolder = org.junit.rules.TemporaryFolder()

    private class FakeCryptoManager : CryptoManager() {
        var encryptCallCount = 0
        var decryptCallCount = 0

        override fun generateSecretKey(): javax.crypto.SecretKey =
            SecretKeySpec(ByteArray(32), "AES")

        override fun removeKey() {}

        override fun encrypt(plaintext: String): EncryptedPayload {
            encryptCallCount++
            return EncryptedPayload(
                ciphertextBase64 = "cipher_$plaintext",
                ivBase64 = "iv_fixed",
            )
        }

        override fun decrypt(ciphertextBase64: String, ivBase64: String): String {
            decryptCallCount++
            return ciphertextBase64.removePrefix("cipher_")
        }
    }

    /**
     * Phase 4: Real Rotation Test.
     * Proves:
     * (a) First run with build key A returns A and stores ciphertext + fingerprint(A).
     * (b) Second run with build key B, same store, returns B and replaces both.
     * (c) Third run with key B again does NOT re-encrypt (encryptCallCount remains unchanged).
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun keyRotationLifecycle_rotatesAndPersists_withoutRedundantReEncryption() = kotlinx.coroutines.test.runTest {
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        val testScope = kotlinx.coroutines.test.TestScope(testDispatcher + kotlinx.coroutines.Job())
        val testFile = temporaryFolder.newFile("test_api_key.preferences_pb")
        val dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile },
        )
        val fakeCrypto = FakeCryptoManager()

        val keyA = "TEST_ROTATION_KEY_A_11111"
        val keyB = "TEST_ROTATION_KEY_B_22222"
        var currentBuildKey = keyA

        // (a) First run with build key A
        val providerA = SecureApiKeyProvider(
            cryptoManager = fakeCrypto,
            dataStore = dataStore,
            ioDispatcher = testDispatcher,
            buildKeyProvider = { currentBuildKey },
        )
        val resolvedA = providerA.getApiKey()
        assertEquals(keyA, resolvedA)
        assertEquals(1, fakeCrypto.encryptCallCount)
        val prefsAfterA = dataStore.data.first()
        assertEquals("cipher_$keyA", prefsAfterA[KEY_CIPHERTEXT])
        assertEquals(SecureApiKeyProvider.sha256HexOf(keyA), prefsAfterA[KEY_BUILD_FINGERPRINT])

        // (b) Second run with build key B (developer updated local.properties and rebuilt)
        currentBuildKey = keyB
        val providerB = SecureApiKeyProvider(
            cryptoManager = fakeCrypto,
            dataStore = dataStore,
            ioDispatcher = testDispatcher,
            buildKeyProvider = { currentBuildKey },
        )
        val resolvedB = providerB.getApiKey()
        assertEquals(keyB, resolvedB)
        assertEquals(2, fakeCrypto.encryptCallCount)
        val prefsAfterB = dataStore.data.first()
        assertEquals("cipher_$keyB", prefsAfterB[KEY_CIPHERTEXT])
        assertEquals(SecureApiKeyProvider.sha256HexOf(keyB), prefsAfterB[KEY_BUILD_FINGERPRINT])

        // (c) Third run: same key B on a subsequent app launch does NOT re-encrypt
        val providerB2 = SecureApiKeyProvider(
            cryptoManager = fakeCrypto,
            dataStore = dataStore,
            ioDispatcher = testDispatcher,
            buildKeyProvider = { currentBuildKey },
        )
        val resolvedB2 = providerB2.getApiKey()
        assertEquals(keyB, resolvedB2)
        assertEquals("Re-encryption must NOT occur when key is unchanged", 2, fakeCrypto.encryptCallCount)
        assertEquals(1, fakeCrypto.decryptCallCount)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun corruptedCiphertext_badTag_safelyReseedsFromBuildConfig() = kotlinx.coroutines.test.runTest {
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        val testScope = kotlinx.coroutines.test.TestScope(testDispatcher + kotlinx.coroutines.Job())
        val testFile = temporaryFolder.newFile("corrupted_cipher.preferences_pb")
        val dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile },
        )
        val throwingCrypto = object : CryptoManager() {
            var encryptCount = 0
            override fun generateSecretKey(): javax.crypto.SecretKey = SecretKeySpec(ByteArray(32), "AES")
            override fun removeKey() {}
            override fun encrypt(plaintext: String): EncryptedPayload {
                encryptCount++
                return EncryptedPayload("reseeded_$plaintext", "iv_reseed")
            }
            override fun decrypt(ciphertextBase64: String, ivBase64: String): String {
                throw javax.crypto.AEADBadTagException("Corrupted tag / invalid authentication tag")
            }
        }

        // Seed with corrupted ciphertext and same fingerprint
        val validKey = "VALID_BUILD_KEY_12345"
        dataStore.edit { p ->
            p[KEY_CIPHERTEXT] = "corrupted_bytes"
            p[KEY_IV] = "corrupted_iv"
            p[KEY_BUILD_FINGERPRINT] = SecureApiKeyProvider.sha256HexOf(validKey)
        }

        val provider = SecureApiKeyProvider(
            cryptoManager = throwingCrypto,
            dataStore = dataStore,
            ioDispatcher = testDispatcher,
            buildKeyProvider = { validKey },
        )

        val resolved = provider.getApiKey()
        assertEquals("Must reseed and return build key on AEADBadTagException", validKey, resolved)
        assertEquals(1, throwingCrypto.encryptCount)
        val prefs = dataStore.data.first()
        assertEquals("reseeded_$validKey", prefs[KEY_CIPHERTEXT])
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun missingKeystoreKey_safelyReseedsFromBuildConfig() = kotlinx.coroutines.test.runTest {
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        val testScope = kotlinx.coroutines.test.TestScope(testDispatcher + kotlinx.coroutines.Job())
        val testFile = temporaryFolder.newFile("missing_keystore_key.preferences_pb")
        val dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile },
        )
        val invalidKeyCrypto = object : CryptoManager() {
            var encryptCount = 0
            override fun generateSecretKey(): javax.crypto.SecretKey = SecretKeySpec(ByteArray(32), "AES")
            override fun removeKey() {}
            override fun encrypt(plaintext: String): EncryptedPayload {
                encryptCount++
                return EncryptedPayload("re-encrypted_$plaintext", "iv_new")
            }
            override fun decrypt(ciphertextBase64: String, ivBase64: String): String {
                throw java.security.InvalidKeyException("Keystore key entry lost or unrecoverable")
            }
        }

        val validKey = "VALID_BUILD_KEY_54321"
        dataStore.edit { p ->
            p[KEY_CIPHERTEXT] = "old_cipher"
            p[KEY_IV] = "old_iv"
            p[KEY_BUILD_FINGERPRINT] = SecureApiKeyProvider.sha256HexOf(validKey)
        }

        val provider = SecureApiKeyProvider(
            cryptoManager = invalidKeyCrypto,
            dataStore = dataStore,
            ioDispatcher = testDispatcher,
            buildKeyProvider = { validKey },
        )

        val resolved = provider.getApiKey()
        assertEquals("Must reseed and return build key on InvalidKeyException", validKey, resolved)
        assertEquals(1, invalidKeyCrypto.encryptCount)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun blankBuildKey_returnsNullAndClearsStoredPreferences() = kotlinx.coroutines.test.runTest {
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        val testScope = kotlinx.coroutines.test.TestScope(testDispatcher + kotlinx.coroutines.Job())
        val testFile = temporaryFolder.newFile("blank_key.preferences_pb")
        val dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile },
        )
        val fakeCrypto = FakeCryptoManager()

        // Seed with existing ciphertext
        dataStore.edit { p ->
            p[KEY_CIPHERTEXT] = "cipher_old"
            p[KEY_IV] = "iv_old"
            p[KEY_BUILD_FINGERPRINT] = "fingerprint_old"
        }

        val provider = SecureApiKeyProvider(
            cryptoManager = fakeCrypto,
            dataStore = dataStore,
            ioDispatcher = testDispatcher,
            buildKeyProvider = { "   " }, // Blank key
        )

        val resolved = provider.getApiKey()
        org.junit.Assert.assertNull("Blank build key must resolve to null", resolved)

        val prefs = dataStore.data.first()
        org.junit.Assert.assertNull(prefs[KEY_CIPHERTEXT])
        org.junit.Assert.assertNull(prefs[KEY_IV])
        org.junit.Assert.assertNull(prefs[KEY_BUILD_FINGERPRINT])
    }
}
