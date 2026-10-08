package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)

interface PreferencesRepository {
    val settings: Flow<AppPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/** User preferences stored with Preferences DataStore. */
class DataStorePreferencesRepository(context: Context) : PreferencesRepository {

    private val store = context.settingsDataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    override val settings: Flow<AppPreferences> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppPreferences(
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }
}
