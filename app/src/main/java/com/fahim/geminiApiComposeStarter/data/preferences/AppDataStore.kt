package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single application-wide Preferences DataStore instance.
 * Reused safely between the security layer (encrypted API key ciphertext & IV)
 * and user preferences (theme selection) without duplicate DataStore instances.
 */
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "gemini_secure_prefs")
