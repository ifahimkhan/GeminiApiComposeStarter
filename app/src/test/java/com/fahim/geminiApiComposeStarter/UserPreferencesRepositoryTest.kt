package com.fahim.geminiApiComposeStarter

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testCoroutineScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        testCoroutineScope = CoroutineScope(Dispatchers.IO + Job())
        val testFile = tempFolder.newFile("test_user_preferences.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testCoroutineScope,
            produceFile = { testFile }
        )
        repository = UserPreferencesRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        testCoroutineScope.cancel()
    }

    @Test
    fun defaultPreferenceValuesAreCorrect() = runTest {
        val initialDarkTheme = repository.isDarkTheme.first()
        assertFalse("Default theme preference should be false", initialDarkTheme)
    }

    @Test
    fun preferenceUpdatesPersistAndCanBeReadBack() = runTest {
        repository.setDarkTheme(true)
        val updatedDarkTheme = repository.isDarkTheme.first()
        assertTrue("Updated theme preference should be true", updatedDarkTheme)

        repository.setDarkTheme(false)
        val revertedDarkTheme = repository.isDarkTheme.first()
        assertFalse("Reverted theme preference should be false", revertedDarkTheme)
    }

    @Test
    fun preferencesPersistAcrossRepositoryRecreation() = runTest {
        val testFile = tempFolder.newFile("persistent_preferences.preferences_pb")
        val scope1 = CoroutineScope(Dispatchers.IO + Job())
        val dataStore1 = PreferenceDataStoreFactory.create(
            scope = scope1,
            produceFile = { testFile }
        )
        val repo1 = UserPreferencesRepositoryImpl(dataStore1)
        repo1.setDarkTheme(true)
        assertEquals(true, repo1.isDarkTheme.first())
        scope1.cancel()

        // Create new repository instance pointing to the exact same file
        val scope2 = CoroutineScope(Dispatchers.IO + Job())
        val dataStore2 = PreferenceDataStoreFactory.create(
            scope = scope2,
            produceFile = { testFile }
        )
        val repo2 = UserPreferencesRepositoryImpl(dataStore2)
        val restoredTheme = repo2.isDarkTheme.first()
        assertTrue("Persisted value should survive repository recreation", restoredTheme)
        scope2.cancel()
    }

    @Test
    fun onlyExpectedThemeKeyIsStored() = runTest {
        repository.setDarkTheme(true)

        val allPreferences = dataStore.data.first()
        assertEquals(1, allPreferences.asMap().size)
        assertTrue(allPreferences.asMap().containsKey(UserPreferencesRepositoryImpl.KEY_DARK_THEME))
        assertEquals(true, allPreferences[UserPreferencesRepositoryImpl.KEY_DARK_THEME])
    }
}
