package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.security.EncryptedData
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        fakePreferencesRepository = FakeUserPreferencesRepository()
        viewModel = ChatViewModel(
            repository = fakeRepository,
            chatMessageDao = fakeDao,
            userPreferencesRepository = fakePreferencesRepository,
            hasApiKey = true
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onPromptChange updates prompt in state`() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun `onSend with empty prompt sets promptError`() = runTest(testDispatcher) {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun `onSend with valid prompt triggers repository and saves messages`() = runTest(testDispatcher) {
        fakeRepository.shouldReturnSuccess = true
        fakeRepository.responseToReturn = "Hello back from Gemini!"

        viewModel.onPromptChange("Hello!")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.prompt)
        assertEquals(2, state.messages.size)
        assertEquals("Hello!", state.messages[0].text)
        assertTrue(state.messages[0].isFromUser)
        assertEquals("Hello back from Gemini!", state.messages[1].text)
        assertFalse(state.messages[1].isFromUser)
    }

    @Test
    fun `onSend failure sets errorMessage`() = runTest(testDispatcher) {
        fakeRepository.shouldReturnSuccess = false
        fakeRepository.errorMessageToReturn = "Network connection failed"

        viewModel.onPromptChange("Test Error")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network connection failed", state.errorMessage)
    }

    @Test
    fun `onSend when missing API key sets errorMessage`() = runTest(testDispatcher) {
        val noKeyViewModel = ChatViewModel(
            repository = fakeRepository,
            chatMessageDao = fakeDao,
            userPreferencesRepository = fakePreferencesRepository,
            hasApiKey = false
        )

        noKeyViewModel.onPromptChange("Hi")
        noKeyViewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, noKeyViewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onVoiceInputResult appends recognized text`() = runTest(testDispatcher) {
        viewModel.onPromptChange("What is")
        viewModel.onVoiceInputResult("the weather today?")
        assertEquals("What is the weather today?", viewModel.uiState.value.prompt)
    }

    @Test
    fun `onClearChat removes all messages`() = runTest(testDispatcher) {
        fakeDao.insertMessage(ChatMessageEntity(text = "Msg 1", isFromUser = true))
        advanceUntilIdle()

        viewModel.onClearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }
}

// FAKES FOR TESTING

class FakeGeminiRepository : GeminiRepository {
    var shouldReturnSuccess = true
    var responseToReturn = "Response"
    var errorMessageToReturn = "Error"

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldReturnSuccess) Result.success(responseToReturn) else Result.failure(Exception(errorMessageToReturn))
    }

    override suspend fun generateTextWithHistory(
        history: List<Pair<String, Boolean>>,
        prompt: String
    ): Result<String> {
        return generateText(prompt)
    }
}

class FakeChatMessageDao : ChatMessageDao {
    private val messages = mutableListOf<ChatMessageEntity>()
    private val flow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = flow

    override suspend fun insertMessage(message: ChatMessageEntity) {
        messages.add(message)
        flow.value = messages.toList()
    }

    override suspend fun insertMessages(messages: List<ChatMessageEntity>) {
        this.messages.addAll(messages)
        flow.value = this.messages.toList()
    }

    override suspend fun clearAllMessages() {
        messages.clear()
        flow.value = emptyList()
    }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val autoScrollFlow = MutableStateFlow(true)
    private val darkModeFlow = MutableStateFlow<Boolean?>(false)
    private val encryptedKeyFlow = MutableStateFlow<EncryptedData?>(null)

    override val autoScrollEnabled: Flow<Boolean> = autoScrollFlow
    override val isDarkMode: Flow<Boolean?> = darkModeFlow
    override val encryptedApiKeyData: Flow<EncryptedData?> = encryptedKeyFlow

    override suspend fun setAutoScrollEnabled(enabled: Boolean) {
        autoScrollFlow.value = enabled
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        darkModeFlow.value = enabled
    }

    override suspend fun saveEncryptedApiKey(encryptedData: EncryptedData) {
        encryptedKeyFlow.value = encryptedData
    }
}
