package com.vaishnavi.n085.geminichat.data.prefs

import kotlinx.coroutines.flow.Flow

/** Persisted copy of the encrypted API key (IV + ciphertext, both Base64). */
data class ApiKeyCipher(val iv: String, val ciphertext: String)

/**
 * Abstraction over user preferences so the ViewModel and ApiKeyStore can be
 * unit-tested against a fake instead of the real DataStore.
 */
interface AppPreferences {
    val userName: Flow<String>
    val apiKeyCipher: Flow<ApiKeyCipher?>
    suspend fun setUserName(name: String)
    suspend fun saveApiKeyCipher(iv: String, ciphertext: String)
}
