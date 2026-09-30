package com.fahim.geminiApiComposeStarter.security

class SecureApiKeyProvider(
    private val storage: ApiKeyStorage,
    private val cryptoManager: ApiKeyCryptoManager
) {

    suspend fun getApiKey(buildConfigApiKey: String): String {
        var encryptedData = storage.read()

        // First launch: encrypt the BuildConfig key and persist only ciphertext.
        if (encryptedData == null) {
            require(buildConfigApiKey.isNotBlank()) {
                "Gemini API key is missing"
            }

            encryptedData = cryptoManager.encrypt(buildConfigApiKey)
            storage.save(encryptedData)
        }

        // Plaintext exists only in memory when requested.
        return cryptoManager.decrypt(encryptedData)
    }
}