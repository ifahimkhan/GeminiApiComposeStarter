package com.fahim.geminiApiComposeStarter.data.local

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeChatDao : ChatDao {
        private val listFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

        override fun getAllMessages(): Flow<List<ChatMessageEntity>> = listFlow

        override fun getAllMessagesSnapshot(): List<ChatMessageEntity> = listFlow.value

        override fun insertMessage(message: ChatMessageEntity) {
            listFlow.value = listFlow.value + message
        }

        override fun insertMessages(messages: List<ChatMessageEntity>) {
            listFlow.value = listFlow.value + messages
        }

        override fun clearAllMessages() {
            listFlow.value = emptyList()
        }
    }

    @Test
    fun getChatHistory_convertsEntitiesToDomainModels() = runTest(testDispatcher) {
        val fakeDao = FakeChatDao()
        fakeDao.insertMessage(
            ChatMessageEntity(id = "1", text = "Hello", participant = "USER", timestamp = 1000L)
        )
        fakeDao.insertMessage(
            ChatMessageEntity(id = "2", text = "Hi there", participant = "ASSISTANT", timestamp = 2000L)
        )

        val repo = ChatHistoryRepositoryImpl(fakeDao, testDispatcher)
        val history = repo.getChatHistory().first()

        assertEquals(2, history.size)
        assertEquals("Hello", history[0].text)
        assertEquals(Participant.USER, history[0].participant)
        assertEquals("Hi there", history[1].text)
        assertEquals(Participant.ASSISTANT, history[1].participant)
    }

    @Test
    fun insertMessage_delegatesToDao() = runTest(testDispatcher) {
        val fakeDao = FakeChatDao()
        val repo = ChatHistoryRepositoryImpl(fakeDao, testDispatcher)

        val msg = ChatMessage(id = "msg-1", text = "Testing insert", participant = Participant.USER, timestamp = 500L)
        repo.insertMessage(msg)

        val stored = fakeDao.getAllMessagesSnapshot()
        assertEquals(1, stored.size)
        assertEquals("msg-1", stored[0].id)
        assertEquals("Testing insert", stored[0].text)
        assertEquals("USER", stored[0].participant)
    }

    @Test
    fun clearHistory_delegatesToDao() = runTest(testDispatcher) {
        val fakeDao = FakeChatDao()
        fakeDao.insertMessage(
            ChatMessageEntity(id = "1", text = "To be deleted", participant = "USER", timestamp = 1000L)
        )

        val repo = ChatHistoryRepositoryImpl(fakeDao, testDispatcher)
        assertEquals(1, fakeDao.getAllMessagesSnapshot().size)

        repo.clearHistory()
        assertTrue(fakeDao.getAllMessagesSnapshot().isEmpty())
    }
}
