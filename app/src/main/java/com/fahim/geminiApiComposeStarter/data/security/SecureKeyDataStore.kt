package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.secureKeyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "secure_keys",
)