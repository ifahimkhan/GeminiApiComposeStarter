package com.fahim.geminiApiComposeStarter.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.fahim.geminiApiComposeStarter.data.security.KEY_CIPHERTEXT
import com.fahim.geminiApiComposeStarter.data.security.KEY_IV
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepositoryImpl

    @Before
    fun setUp() {
        val testFile = temporaryFolder.newFile("test_user_prefs.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile },
        )
        repository = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun defaultThemeMode_isSystem() = runTest(testDispatcher) {
        val theme = repository.themeMode.first()
        assertEquals(ThemeMode.SYSTEM, theme)
    }

    @Test
    fun saveThemeMode_light_persistsLight() = runTest(testDispatcher) {
        repository.setThemeMode(ThemeMode.LIGHT)
        val theme = repository.themeMode.first()
        assertEquals(ThemeMode.LIGHT, theme)
    }

    @Test
    fun saveThemeMode_dark_persistsDark() = runTest(testDispatcher) {
        repository.setThemeMode(ThemeMode.DARK)
        val theme = repository.themeMode.first()
        assertEquals(ThemeMode.DARK, theme)
    }

    @Test
    fun saveThemeMode_system_persistsSystem() = runTest(testDispatcher) {
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun reloadingFromDataStore_restoresSavedTheme() = runTest(testDispatcher) {
        repository.setThemeMode(ThemeMode.DARK)

        val newRepo = UserPreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher,
        )
        val restoredTheme = newRepo.themeMode.first()
        assertEquals(ThemeMode.DARK, restoredTheme)
    }

    @Test
    fun invalidStoredValue_safelyFallsBackToSystem() = runTest(testDispatcher) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = "INVALID_THEME_STRING"
        }

        val theme = repository.themeMode.first()
        assertEquals(ThemeMode.SYSTEM, theme)
    }

    @Test
    fun preferenceIsolation_fromSecurityStorage() = runTest(testDispatcher) {
        // Seed security keys as SecureApiKeyProvider does
        dataStore.edit { prefs ->
            prefs[KEY_CIPHERTEXT] = "encrypted_dummy_ciphertext"
            prefs[KEY_IV] = "dummy_iv_base64"
        }

        // Save user theme preference
        repository.setThemeMode(ThemeMode.DARK)

        // Verify security keys remain intact and isolated
        val prefs = dataStore.data.first()
        assertEquals("encrypted_dummy_ciphertext", prefs[KEY_CIPHERTEXT])
        assertEquals("dummy_iv_base64", prefs[KEY_IV])
        assertEquals("DARK", prefs[KEY_THEME_MODE])
        assertEquals(ThemeMode.DARK, repository.themeMode.first())

        // Mutate security key and verify user preference is unaffected
        dataStore.edit { p ->
            p[KEY_CIPHERTEXT] = "updated_ciphertext"
        }
        val updatedPrefs = dataStore.data.first()
        assertEquals("updated_ciphertext", updatedPrefs[KEY_CIPHERTEXT])
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }
}
