package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.FakeChatMessageDao
import com.fahim.geminiApiComposeStarter.data.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.data.FakeThemePreferencesRepository
import com.fahim.geminiApiComposeStarter.data.Participant
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var fakeThemeRepo: FakeThemePreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        fakeThemeRepo = FakeThemePreferencesRepository()
        viewModel = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = true,
            chatMessageDao = fakeDao,
            themePreferencesRepository = fakeThemeRepo,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() {
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_blankPrompt_isRejectedWithoutCallingRepository() = runTest(testDispatcher) {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertEquals(0, fakeRepository.callCount)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_addsUserMessageAndClearsPromptInputImmediately() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        // Prompt input is cleared immediately
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        // User message is present in UI state and persisted
        val messages = viewModel.uiState.value.messages
        assertTrue(messages.any { it.text == "Hello Gemini" && it.participant == Participant.USER })
    }

    @Test
    fun onSend_successfulGeminiResponse_appendsModelMessageAndStopsLoading() = runTest(testDispatcher) {
        fakeRepository.resultToReturn = Result.success("Hello, I am Gemini!")

        viewModel.onPromptChange("Who are you?")
        viewModel.onSend()

        // While pending execution, loading state is exposed
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // Loading completes and model response is appended
        assertFalse(viewModel.uiState.value.isLoading)
        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)

        assertEquals("Who are you?", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)

        assertEquals("Hello, I am Gemini!", messages[1].text)
        assertEquals(Participant.MODEL, messages[1].participant)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSend_repositoryError_setsErrorMessageAndStopsLoading() = runTest(testDispatcher) {
        val errorMessage = "Quota exceeded or network timeout"
        fakeRepository.resultToReturn = Result.failure(RuntimeException(errorMessage))

        viewModel.onPromptChange("Generate something")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(errorMessage, viewModel.uiState.value.errorMessage)

        // Only the user's message was appended; no model message was added
        val messages = viewModel.uiState.value.messages
        assertEquals(1, messages.size)
        assertEquals(Participant.USER, messages[0].participant)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessageWithoutCallingRepository() = runTest(testDispatcher) {
        val noKeyViewModel = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = false,
            chatMessageDao = fakeDao,
            ioDispatcher = testDispatcher,
        )

        noKeyViewModel.onPromptChange("Hello")
        noKeyViewModel.onSend()
        advanceUntilIdle()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, noKeyViewModel.uiState.value.errorMessage)
        assertEquals(0, fakeRepository.callCount)
        assertTrue(noKeyViewModel.uiState.value.messages.isEmpty())
    }

    // --- Stage 6: Room Persistence Tests ---

    @Test
    fun init_loadsPreviouslyStoredMessagesFromDao() = runTest(testDispatcher) {
        val preloadedMessages = listOf(
            ChatMessageEntity(id = "1", text = "Previous user message", participant = "USER", timestamp = 1000L),
            ChatMessageEntity(id = "2", text = "Previous model reply", participant = "MODEL", timestamp = 2000L),
        )
        val loadedDao = FakeChatMessageDao(preloadedMessages)

        val vmWithHistory = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = true,
            chatMessageDao = loadedDao,
            ioDispatcher = testDispatcher,
        )

        advanceUntilIdle()

        val stateMessages = vmWithHistory.uiState.value.messages
        assertEquals(2, stateMessages.size)
        assertEquals("Previous user message", stateMessages[0].text)
        assertEquals(Participant.USER, stateMessages[0].participant)
        assertEquals("Previous model reply", stateMessages[1].text)
        assertEquals(Participant.MODEL, stateMessages[1].participant)
    }

    @Test
    fun onSend_persistsUserMessageToDao() = runTest(testDispatcher) {
        viewModel.onPromptChange("Message to persist")
        viewModel.onSend()
        advanceUntilIdle()

        val stored = fakeDao.getAllMessagesSnapshot()
        assertTrue(stored.any { it.text == "Message to persist" && it.participant == "USER" })
    }

    @Test
    fun onSend_persistsModelResponseToDao() = runTest(testDispatcher) {
        fakeRepository.resultToReturn = Result.success("Persisted AI response")

        viewModel.onPromptChange("Prompt")
        viewModel.onSend()
        advanceUntilIdle()

        val stored = fakeDao.getAllMessagesSnapshot()
        assertEquals(2, stored.size)
        assertEquals("Prompt", stored[0].text)
        assertEquals("USER", stored[0].participant)
        assertEquals("Persisted AI response", stored[1].text)
        assertEquals("MODEL", stored[1].participant)
    }

    @Test
    fun onSend_repositoryError_doesNotPersistFailedModelResponse() = runTest(testDispatcher) {
        fakeRepository.resultToReturn = Result.failure(RuntimeException("Network error"))

        viewModel.onPromptChange("Failing prompt")
        viewModel.onSend()
        advanceUntilIdle()

        val stored = fakeDao.getAllMessagesSnapshot()
        assertEquals(1, stored.size)
        assertEquals("Failing prompt", stored[0].text)
        assertEquals("USER", stored[0].participant)
        assertTrue(stored.none { it.participant == "MODEL" })
    }

    // --- Stage 7: Preferences DataStore & Theme Toggle Tests ---

    @Test
    fun init_defaultTheme_isLightWhenNoPreferenceStored() = runTest(testDispatcher) {
        val vm = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = true,
            chatMessageDao = fakeDao,
            themePreferencesRepository = FakeThemePreferencesRepository(initialDarkMode = null),
            ioDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isDarkTheme)
    }

    @Test
    fun init_loadsSavedDarkModePreference() = runTest(testDispatcher) {
        val vm = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = true,
            chatMessageDao = fakeDao,
            themePreferencesRepository = FakeThemePreferencesRepository(initialDarkMode = true),
            ioDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isDarkTheme)
    }

    @Test
    fun toggleTheme_updatesUiStateAndPersistsPreference() = runTest(testDispatcher) {
        assertFalse(viewModel.uiState.value.isDarkTheme)

        viewModel.toggleTheme()
        assertTrue(viewModel.uiState.value.isDarkTheme)

        advanceUntilIdle()
        assertEquals(1, fakeThemeRepo.setDarkModeCallCount)

        viewModel.toggleTheme()
        assertFalse(viewModel.uiState.value.isDarkTheme)

        advanceUntilIdle()
        assertEquals(2, fakeThemeRepo.setDarkModeCallCount)
    }

    // --- Stage 8: Voice Input Tests ---

    @Test
    fun onVoiceInputResult_updatesPromptAndClearsPromptError() {
        viewModel.onPromptChange("Initial")
        viewModel.onVoiceInputResult("What is machine learning?")

        assertEquals("What is machine learning?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onVoiceInputResult_blankText_doesNotOverwriteExistingPrompt() {
        viewModel.onPromptChange("Keep this prompt")
        viewModel.onVoiceInputResult("   ")

        assertEquals("Keep this prompt", viewModel.uiState.value.prompt)
    }

    @Test
    fun onVoiceRecognitionError_setsErrorMessage() {
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.onVoiceRecognitionError("Speech recognition is not available on this device")

        assertEquals(
            "Speech recognition is not available on this device",
            viewModel.uiState.value.errorMessage
        )
    }
}
