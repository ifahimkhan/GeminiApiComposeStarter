package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val selectedModelFlow: Flow<String>
    suspend fun setSelectedModel(modelName: String)

    companion object {
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_gemini_model")
        const val DEFAULT_MODEL = "gemini-3.6-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-3.6-flash",
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite",
        )
    }
}

class UserPreferencesRepositoryImpl(
    private val context: Context,
) : UserPreferencesRepository {

    private val dataStore = context.dataStore

    override val selectedModelFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val savedModel = preferences[UserPreferencesRepository.KEY_SELECTED_MODEL]
            // If the persisted model is missing or was set to an unsupported/retired model
            // (e.g. legacy gemini-1.5-flash), automatically fall back to the supported DEFAULT_MODEL.
            if (savedModel != null && UserPreferencesRepository.AVAILABLE_MODELS.contains(savedModel)) {
                savedModel
            } else {
                UserPreferencesRepository.DEFAULT_MODEL
            }
        }

    override suspend fun setSelectedModel(modelName: String) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesRepository.KEY_SELECTED_MODEL] = modelName
        }
    }
}
