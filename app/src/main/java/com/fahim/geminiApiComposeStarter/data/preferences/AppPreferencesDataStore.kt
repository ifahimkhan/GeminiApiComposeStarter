package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single top-level DataStore instance reused across the application. */
val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")
