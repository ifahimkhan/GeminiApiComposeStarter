package com.fahim.geminiApiComposeStarter.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fahim.geminiApiComposeStarter.data.model.ResponseStyle
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import com.fahim.geminiApiComposeStarter.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setResponseStyle(style: ResponseStyle)
}

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { p ->
        UserPreferences(
            themeMode = p[THEME_MODE].toEnumOr(ThemeMode.SYSTEM),
            dynamicColor = p[DYNAMIC_COLOR] ?: true,
            responseStyle = p[RESPONSE_STYLE].toEnumOr(ResponseStyle.BALANCED),
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setResponseStyle(style: ResponseStyle) {
        dataStore.edit { it[RESPONSE_STYLE] = style.name }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val RESPONSE_STYLE = stringPreferencesKey("response_style")
    }
}
