package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

interface PreferencesStore {

    val showUserMessages: Flow<Boolean>

    val encryptedGeminiApiKey: Flow<String?>

    suspend fun setShowUserMessages(
        enabled: Boolean
    )

    suspend fun saveEncryptedGeminiApiKey(
        encryptedApiKey: String
    )
}