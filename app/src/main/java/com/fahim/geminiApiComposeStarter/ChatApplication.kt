package com.fahim.geminiApiComposeStarter

import android.app.Application
import androidx.room.Room
import com.fahim.geminiApiComposeStarter.data.ChatDatabase
import com.fahim.geminiApiComposeStarter.data.DataStoreUserPreferences
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.RoomConversationRepository
import com.fahim.geminiApiComposeStarter.data.SecureApiKeyStore

class ChatApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, ChatDatabase::class.java, "conversation.db").build()
    }
    val conversations by lazy { RoomConversationRepository(database.chatDao()) }
    val preferences by lazy { DataStoreUserPreferences(this) }
    val secureKeyStore by lazy { SecureApiKeyStore(this) }
    val gemini by lazy { GeminiRepositoryImpl(secureKeyStore) }
}
