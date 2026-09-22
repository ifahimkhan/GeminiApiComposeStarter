package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class AiTone(val displayName: String, val promptInstruction: String) {
    BALANCED("Balanced", "Provide helpful, balanced, and clear answers."),
    CONCISE("Concise", "Provide ultra-concise, direct answers without unnecessary filler."),
    CREATIVE("Creative", "Respond creatively, with engaging examples and vivid phrasing."),
    TECHNICAL("Technical", "Provide in-depth technical explanations with code or structured breakdowns."),
}

interface UserPreferencesRepository {
    val selectedToneFlow: Flow<AiTone>
    suspend fun setAiTone(tone: AiTone)
}

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    private object PreferencesKeys {
        val SELECTED_TONE = stringPreferencesKey("selected_ai_tone")
    }

    override val selectedToneFlow: Flow<AiTone> = context.userPrefsDataStore.data.map { prefs ->
        val savedName = prefs[PreferencesKeys.SELECTED_TONE]
        AiTone.entries.find { it.name == savedName } ?: AiTone.BALANCED
    }

    override suspend fun setAiTone(tone: AiTone) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_TONE] = tone.name
        }
    }
}
