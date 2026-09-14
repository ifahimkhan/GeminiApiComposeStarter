package com.fahim.geminiApiComposeStarter.data.datastore

import kotlinx.coroutines.flow.Flow

/** Abstraction over persisted user preferences so the ViewModel can be unit tested. */
interface UserPreferencesRepository {
    val useDynamicColor: Flow<Boolean>
    suspend fun setUseDynamicColor(enabled: Boolean)
}
