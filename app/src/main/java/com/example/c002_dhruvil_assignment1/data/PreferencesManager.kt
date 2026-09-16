package com.example.c002_dhruvil_assignment1.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class PreferencesManager(private val context: Context) {
    companion object {
        val SELECTED_IMAGE_KEY = intPreferencesKey("selected_image_index")
    }

    val selectedImageIndex: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_IMAGE_KEY] ?: 0
        }

    suspend fun saveSelectedImageIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_IMAGE_KEY] = index
        }
    }
}
