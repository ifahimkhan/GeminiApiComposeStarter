package com.fahim.geminiApiComposeStarter

import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.security.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.security.KeyStore
import java.util.UUID

class ApiKeyVaultTest {
    @Test fun ciphertextRoundTripRotationAndTamperRecovery() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = UUID.randomUUID().toString()
        val file = File(context.cacheDir, "$name.preferences_pb")
        val alias = "test-$name"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val fake = "test-only-not-a-real-api-key"
        try {
            val vault = ApiKeyVault(context, { fake }, "1", store, alias)
            vault.initialize()
            val first = store.data.first()
            assertFalse(first.asMap().values.contains(fake))
            assertTrue(vault.withCredential { it == fake })
            val rotated = ApiKeyVault(context, { fake }, "2", store, alias)
            rotated.initialize()
            assertNotEquals(first[stringPreferencesKey("iv")], store.data.first()[stringPreferencesKey("iv")])
            store.edit {
                val bytes = Base64.decode(it[stringPreferencesKey("ciphertext")], Base64.NO_WRAP)
                bytes[0] = (bytes[0].toInt() xor 1).toByte()
                it[stringPreferencesKey("ciphertext")] = Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            assertTrue(rotated.withCredential { it == fake })
        } finally {
            scope.cancel(); KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }; file.delete()
        }
    }
    @Test fun missingKeyNeverCreatesCredentialEnvelope() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            try { ApiKeyVault(context, { "" }, "1", store, "unused-test-alias").initialize(); fail("Expected missing credentials") }
            catch (_: MissingCredentialException) { assertTrue(store.data.first().asMap().isEmpty()) }
        } finally { scope.cancel(); file.delete() }
    }
}
