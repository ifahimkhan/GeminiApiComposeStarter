package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.chatPreferences by preferencesDataStore(name = "chat_preferences")

class AppPreferences(context: Context) {
    private val store = context.applicationContext.chatPreferences
    private val keyboard = booleanPreferencesKey("open_keyboard")
    private val model = stringPreferencesKey("gemini_model")
    private val active = stringPreferencesKey("active_conversation")
    val values = store.data.map {
        Settings(it[keyboard] ?: true, it[model] ?: "gemini-3.6-flash", it[active])
    }
    suspend fun save(openKeyboard: Boolean, modelName: String) {
        store.edit { it[keyboard] = openKeyboard; it[model] = modelName.trim() }
    }
    suspend fun select(id: String) { store.edit { it[active] = id } }
}

data class Settings(val openKeyboard: Boolean = true, val modelName: String = "gemini-3.6-flash", val activeId: String? = null)
