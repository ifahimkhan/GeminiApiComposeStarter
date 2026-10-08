package com.fahim.geminiApiComposeStarter

import android.content.Context
import com.fahim.geminiApiComposeStarter.data.MessageRepository
import com.fahim.geminiApiComposeStarter.data.DataStorePreferencesRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomMessageRepository
import com.fahim.geminiApiComposeStarter.data.PreferencesRepository
import com.fahim.geminiApiComposeStarter.data.local.MessageDatabase
import com.fahim.geminiApiComposeStarter.security.SecureKeyStore

/** Tiny manual dependency-injection container (no Hilt needed for a lab). */
class ServiceLocator(context: Context) {
    private val appContext = context.applicationContext

    val secureKeyStore = SecureKeyStore(appContext)

    private val database by lazy { MessageDatabase.create(appContext) }

    val geminiRepository: GeminiRepository by lazy { GeminiRepositoryImpl(secureKeyStore) }
    val messageRepository: MessageRepository by lazy {
        RoomMessageRepository(database.messageDao())
    }
    val preferencesRepository: PreferencesRepository by lazy { DataStorePreferencesRepository(appContext) }
}
