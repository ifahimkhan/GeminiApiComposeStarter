package com.example.n083harshitraiassignment1.ui.chat

import android.app.Application
import android.content.Context
import com.example.n083harshitraiassignment1.data.ChatDao
import com.example.n083harshitraiassignment1.data.GeminiRepository
import com.example.n083harshitraiassignment1.data.LocalChatRepository
import com.example.n083harshitraiassignment1.data.SecureApiKeyStorage
import com.example.n083harshitraiassignment1.data.UserPreferences
import com.example.n083harshitraiassignment1.data.UserPreferencesRepository
import com.example.n083harshitraiassignment1.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Test fakes
    private lateinit var fakeChatDao: FakeChatDao
    private lateinit var fakeLocalRepo: LocalChatRepository
    private lateinit var fakeGeminiRepo: FakeGeminiRepository
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository
    private lateinit var fakeStorage: FakeSecureApiKeyStorage
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val app = Application()
        fakeChatDao = FakeChatDao()
        fakeLocalRepo = LocalChatRepository(fakeChatDao)
        fakeGeminiRepo = FakeGeminiRepository()
        fakePrefsRepo = FakeUserPreferencesRepository()
        fakeStorage = FakeSecureApiKeyStorage()

        viewModel = ChatViewModel(
            application = app,
            localRepository = fakeLocalRepo,
            secureStorage = fakeStorage,
            userPreferencesRepository = fakePrefsRepo,
            geminiRepository = fakeGeminiRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasEmptyInputAndNoErrors() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("", state.inputText)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("TestUser", state.userName)
        assertEquals("Academic", state.tone)
    }

    @Test
    fun updateInput_updatesUiStateInputText() = runTest {
        viewModel.updateInput("Explain relativity in simple terms")

        val state = viewModel.uiState.value
        assertEquals("Explain relativity in simple terms", state.inputText)
    }

    @Test
    fun clearError_resetsErrorMessageToNull() = runTest {
        // Trigger an error first via a failed message send
        fakeGeminiRepo.shouldSucceed = false
        viewModel.updateInput("Trigger failure")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun sendMessage_emptyText_doesNotTriggerSend() = runTest {
        viewModel.updateInput("   ")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeChatDao.storedMessages.isEmpty())
    }

    @Test
    fun sendMessage_success_savesMessagesAndClearsInput() = runTest {
        fakeGeminiRepo.shouldSucceed = true
        fakeGeminiRepo.cannedResponse = "Gravity is the curvature of spacetime."

        viewModel.updateInput("What is gravity?")
        viewModel.sendMessage()

        // Input should be cleared immediately
        assertEquals("", viewModel.uiState.value.inputText)

        // Advance coroutines to complete network and database calls
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        // Verify both user prompt and AI response are recorded
        val messages = fakeChatDao.storedMessages
        assertEquals(2, messages.size)
        assertEquals("What is gravity?", messages[0].text)
        assertTrue(messages[0].isUser)
        assertEquals("Gravity is the curvature of spacetime.", messages[1].text)
        assertFalse(messages[1].isUser)
    }

    @Test
    fun sendMessage_failure_recordsErrorAndStopsLoading() = runTest {
        fakeGeminiRepo.shouldSucceed = false
        fakeGeminiRepo.errorMessage = "Gemini API rate limit exceeded"

        viewModel.updateInput("A question that will fail")
        viewModel.sendMessage()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Gemini API rate limit exceeded", state.errorMessage)
    }

    @Test
    fun clearHistory_removesAllMessagesFromDao() = runTest {
        fakeChatDao.insertMessage(ChatMessage(text = "Previous message", isUser = true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeChatDao.storedMessages.isEmpty())
    }

    // --- Test Doubles / Fakes ---

    private class FakeChatDao : ChatDao {
        val storedMessages = mutableListOf<ChatMessage>()
        private val flow = MutableStateFlow<List<ChatMessage>>(emptyList())

        override fun getAllMessages(): Flow<List<ChatMessage>> = flow

        override suspend fun insertMessage(message: ChatMessage) {
            storedMessages.add(message)
            flow.value = storedMessages.toList()
        }

        override suspend fun deleteAllMessages() {
            storedMessages.clear()
            flow.value = emptyList()
        }
    }

    private class FakeGeminiRepository : GeminiRepository("test-api-key") {
        var shouldSucceed = true
        var cannedResponse = "Default mock AI response"
        var errorMessage = "Connection failed"

        override suspend fun generateResponse(prompt: String): String {
            if (!shouldSucceed) {
                throw RuntimeException(errorMessage)
            }
            return cannedResponse
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository() {
        private val prefsFlow = MutableStateFlow(
            UserPreferences(
                userName = "TestUser",
                preferredTone = "Academic",
                autoScrollEnabled = true
            )
        )

        override val userPreferencesFlow: Flow<UserPreferences> = prefsFlow
    }

    private class FakeSecureApiKeyStorage : SecureApiKeyStorage() {
        override fun getDecryptedApiKey(fallbackKey: String): String = "mock-decrypted-key"
    }
}
