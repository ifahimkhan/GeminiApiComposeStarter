package com.example.c001manavassignment1.ui.chat

import com.example.c001manavassignment1.data.GeminiRepository
import com.example.c001manavassignment1.data.IGeminiRepository
import com.example.c001manavassignment1.data.UserPreferences
import com.example.c001manavassignment1.data.IUserPreferencesRepository
import com.example.c001manavassignment1.data.local.ConversationEntity
import com.example.c001manavassignment1.security.IApiKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val viewModel = ChatViewModel(FakeApiKeyManager(), FakeUserPreferencesRepository()) { FakeGeminiRepository() }
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertEquals("", state.inputText)
    }

    @Test
    fun `sending message updates state and gets response`() = runTest {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(FakeApiKeyManager("valid_key"), FakeUserPreferencesRepository()) { fakeRepo }
        
        runCurrent()
        
        viewModel.onInputTextChange("Hello")
        viewModel.sendMessage()
        
        runCurrent()
        
        assertEquals("Hello", fakeRepo.lastPrompt)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `empty input does not send message`() = runTest {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(FakeApiKeyManager("valid_key"), FakeUserPreferencesRepository()) { fakeRepo }
        
        runCurrent()
        
        viewModel.onInputTextChange("")
        viewModel.sendMessage()
        
        runCurrent()
        
        assertEquals(null, fakeRepo.lastPrompt)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `Gemini error updates error state`() = runTest {
        val fakeRepo = FakeGeminiRepository(shouldThrow = true)
        val viewModel = ChatViewModel(FakeApiKeyManager("valid_key"), FakeUserPreferencesRepository()) { fakeRepo }
        
        runCurrent()
        
        viewModel.onInputTextChange("Hello")
        viewModel.sendMessage()
        
        runCurrent()
        
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("Gemini Error"))
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `duplicate send is prevented while loading`() = runTest {
        val fakeRepo = FakeGeminiRepository(delayResponse = true)
        val viewModel = ChatViewModel(FakeApiKeyManager("valid_key"), FakeUserPreferencesRepository()) { fakeRepo }
        
        runCurrent()
        
        viewModel.onInputTextChange("First")
        viewModel.sendMessage()
        
        // At this point repo.sendMessage is called and suspended due to delay.
        // isLoading should be true.
        assertTrue(viewModel.uiState.value.isLoading)
        
        viewModel.onInputTextChange("Second")
        viewModel.sendMessage()
        
        // The second prompt should NOT have been processed.
        assertEquals("First", fakeRepo.lastPrompt)
    }

    @Test
    fun `toggle dark mode updates preferences`() = runTest {
        val prefsRepo = FakeUserPreferencesRepository()
        val viewModel = ChatViewModel(FakeApiKeyManager(), prefsRepo) { FakeGeminiRepository() }
        
        runCurrent()
        assertFalse(viewModel.uiState.value.isDarkMode)
        
        viewModel.toggleDarkMode()
        runCurrent()
        
        assertTrue(viewModel.uiState.value.isDarkMode)
    }

    // Fakes
    class FakeApiKeyManager(private val key: String = "") : IApiKeyManager {
        override suspend fun initializeApiKey() {}
        override suspend fun getApiKey(): String = key
    }

    class FakeUserPreferencesRepository : IUserPreferencesRepository {
        private val _flow = MutableStateFlow(UserPreferences(false))
        override val userPreferencesFlow: Flow<UserPreferences> = _flow
        override suspend fun updateDarkMode(isDarkMode: Boolean) {
            _flow.value = UserPreferences(isDarkMode)
        }
    }

    class FakeGeminiRepository(
        private val delayResponse: Boolean = false,
        private val shouldThrow: Boolean = false
    ) : IGeminiRepository {
        var lastPrompt: String? = null
        private val _messages = MutableStateFlow<List<GeminiRepository.ChatMessage>>(emptyList())
        private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
        
        override val allConversations: Flow<List<ConversationEntity>> = _conversations

        override fun getMessagesForConversation(conversationId: Long): Flow<List<GeminiRepository.ChatMessage>> = _messages

        override suspend fun createConversation(title: String): Long {
            val id = (_conversations.value.size + 1).toLong()
            _conversations.value += ConversationEntity(id, title)
            return id
        }

        override suspend fun deleteConversation(conversation: ConversationEntity) {
            _conversations.value = _conversations.value.filter { it.id != conversation.id }
        }

        override suspend fun sendMessage(prompt: String, conversationId: Long): String {
            if (shouldThrow) throw Exception("Test Exception")
            lastPrompt = prompt
            if (delayResponse) kotlinx.coroutines.delay(1000)
            
            val response = "Echo: $prompt"
            val newMessages = _messages.value + 
                GeminiRepository.ChatMessage(System.currentTimeMillis(), prompt, true) + 
                GeminiRepository.ChatMessage(System.currentTimeMillis() + 1, response, false)
            _messages.value = newMessages
            return response
        }
    }
}
