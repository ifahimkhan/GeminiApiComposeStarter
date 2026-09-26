package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException

private val Context.userPreferences by preferencesDataStore("user_preferences")
class PreferencesRepository(context: Context) : PreferenceStore {
    private val store = context.applicationContext.userPreferences
    private val name = stringPreferencesKey("display_name")
    private val category = stringPreferencesKey("preferred_category")
    private val model = stringPreferencesKey("preferred_model")
    private val motion = booleanPreferencesKey("reduced_motion")
    override val preferences = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        UserPreferences(it[name].orEmpty(), Category.entries.firstOrNull { c -> c.name == it[category] } ?: Category.GENERAL, it[motion] ?: false, GeminiModels.normalize(it[model].orEmpty()))
    }
    override suspend fun save(value: UserPreferences) { store.edit {
        it[name] = value.displayName.take(40); it[category] = value.category.name; it[motion] = value.reducedMotion; it[model] = GeminiModels.normalize(value.modelId)
    } }
}
