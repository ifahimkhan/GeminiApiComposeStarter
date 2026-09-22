package com.fahim.geminiApiComposeStarter.data.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** User-visible settings (theme, dynamic colour). Safe to back up. */
val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

/** Holds only the AES-GCM ciphertext of the API key -- never the plaintext. */
val Context.secureDataStore by preferencesDataStore(name = "secure_store")
