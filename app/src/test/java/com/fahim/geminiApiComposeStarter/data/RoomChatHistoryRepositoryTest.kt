package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomChatHistoryRepositoryTest {

    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var repository: RoomChatHistoryRepository

    private class FakeChatMessageDao : ChatMessageDao {
        val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
        val inserted = mutableListOf<ChatMessageEntity>()
        var clearCalled = false

        override fun getAllMessages(): Flow<List<ChatMessageEntity>> = messagesFlow

        override suspend fun insertMessage(message: ChatMessageEntity) {
            inserted.add(message)
            messagesFlow.value = (messagesFlow.value.filterNot { it.id == message.id } + message)
                .sortedBy { it.timestamp }
        }

        override suspend fun insertMessages(messages: List<ChatMessageEntity>) {
            inserted.addAll(messages)
            val updated = messagesFlow.value.toMutableList()
            for (msg in messages) {
                updated.removeAll { it.id == msg.id }
                updated.add(msg)
            }
            messagesFlow.value = updated.sortedBy { it.timestamp }
        }

        override suspend fun clearAllMessages(): Int {
            clearCalled = true
            val count = messagesFlow.value.size
            messagesFlow.value = emptyList()
            return count
        }

        override suspend fun getMessageCount(): Int = messagesFlow.value.size
    }

    @Before
    fun setUp() {
        fakeDao = FakeChatMessageDao()
        repository = RoomChatHistoryRepository(fakeDao)
    }

    @Test
    fun getMessages_delegatesToDao_andMapsEntitiesToDomainMessages() = runTest {
        val entity = ChatMessageEntity(
            id = "test-id",
            text = "Test response",
            participant = "ASSISTANT",
            timestamp = 12345L,
        )
        fakeDao.messagesFlow.value = listOf(entity)

        val messages = repository.getMessages().first()
        assertEquals(1, messages.size)
        assertEquals("test-id", messages[0].id)
        assertEquals("Test response", messages[0].text)
        assertEquals(Participant.ASSISTANT, messages[0].participant)
        assertEquals(12345L, messages[0].timestamp)
    }

    @Test
    fun saveMessage_delegatesToDao_withMappedEntity() = runTest {
        val message = ChatMessage(
            id = "msg-user-1",
            text = "User prompt",
            participant = Participant.USER,
            timestamp = 9999L,
        )

        repository.saveMessage(message)

        assertEquals(1, fakeDao.inserted.size)
        val inserted = fakeDao.inserted[0]
        assertEquals("msg-user-1", inserted.id)
        assertEquals("User prompt", inserted.text)
        assertEquals("USER", inserted.participant)
        assertEquals(9999L, inserted.timestamp)
    }

    @Test
    fun saveMessages_delegatesToDao_withMappedEntities() = runTest {
        val messages = listOf(
            ChatMessage(id = "1", text = "One", participant = Participant.USER, timestamp = 1L),
            ChatMessage(id = "2", text = "Two", participant = Participant.ASSISTANT, timestamp = 2L),
        )

        repository.saveMessages(messages)

        assertEquals(2, fakeDao.inserted.size)
        assertEquals("1", fakeDao.inserted[0].id)
        assertEquals("USER", fakeDao.inserted[0].participant)
        assertEquals("2", fakeDao.inserted[1].id)
        assertEquals("ASSISTANT", fakeDao.inserted[1].participant)
    }

    @Test
    fun clearHistory_delegatesToDao_clearAllMessages() = runTest {
        repository.saveMessage(
            ChatMessage(id = "1", text = "A", participant = Participant.USER)
        )
        repository.clearHistory()

        assertTrue(fakeDao.clearCalled)
        assertTrue(repository.getMessages().first().isEmpty())
    }
}
