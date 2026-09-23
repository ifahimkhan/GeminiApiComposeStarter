package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.modelSelectionDataStore by preferencesDataStore(name = "model_selection")

interface ModelSelectionPreferences {
    val selectedProvider: Flow<AiProvider>
    suspend fun setProvider(provider: AiProvider)
}

class DataStoreModelSelectionPreferences(private val context: Context) : ModelSelectionPreferences {
    private val key = stringPreferencesKey("selected_provider")
    override val selectedProvider: Flow<AiProvider> = context.modelSelectionDataStore.data.map {
        runCatching { AiProvider.valueOf(it[key] ?: AiProvider.GEMINI.name) }
            .getOrDefault(AiProvider.GEMINI)
    }

    override suspend fun setProvider(provider: AiProvider) {
        context.modelSelectionDataStore.edit { it[key] = provider.name }
    }
}
