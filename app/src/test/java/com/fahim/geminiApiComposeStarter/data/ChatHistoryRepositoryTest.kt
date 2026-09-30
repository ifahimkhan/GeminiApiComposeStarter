package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.toChatMessage
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ChatHistoryRepositoryTest {

    private lateinit var repository: InMemoryChatHistoryRepository

    @Before
    fun setUp() {
        repository = InMemoryChatHistoryRepository()
    }

    @Test
    fun insertAndRetrieval_savesAndEmitsMessages() = runTest {
        val userMsg = ChatMessage(
            id = "msg-1",
            text = "Hello Gemini",
            participant = Participant.USER,
            timestamp = 1000L,
        )
        val assistantMsg = ChatMessage(
            id = "msg-2",
            text = "Hello User",
            participant = Participant.ASSISTANT,
            timestamp = 2000L,
        )

        repository.saveMessage(userMsg)
        repository.saveMessage(assistantMsg)

        val messages = repository.getMessages().first()
        assertEquals(2, messages.size)
        assertEquals(userMsg, messages[0])
        assertEquals(assistantMsg, messages[1])
    }

    @Test
    fun ordering_preservesChronologicalOrderRegardlessOfInsertOrder() = runTest {
        val msgLater = ChatMessage(
            id = "msg-late",
            text = "Second message",
            participant = Participant.ASSISTANT,
            timestamp = 5000L,
        )
        val msgEarlier = ChatMessage(
            id = "msg-early",
            text = "First message",
            participant = Participant.USER,
            timestamp = 1000L,
        )

        // Insert in reverse chronological order
        repository.saveMessage(msgLater)
        repository.saveMessage(msgEarlier)

        val messages = repository.getMessages().first()
        assertEquals(2, messages.size)
        assertEquals("First message", messages[0].text)
        assertEquals("Second message", messages[1].text)
        assertTrue(messages[0].timestamp < messages[1].timestamp)
    }

    @Test
    fun duplicatePrevention_updatingExistingMessageReplacesWithoutDuplication() = runTest {
        val initialMsg = ChatMessage(
            id = "stable-id-1",
            text = "Draft response",
            participant = Participant.ASSISTANT,
            timestamp = 1000L,
        )
        val updatedMsg = ChatMessage(
            id = "stable-id-1",
            text = "Final completed response",
            participant = Participant.ASSISTANT,
            timestamp = 1000L,
        )

        repository.saveMessage(initialMsg)
        assertEquals(1, repository.getMessages().first().size)

        repository.saveMessage(updatedMsg)
        val messages = repository.getMessages().first()
        assertEquals(1, messages.size)
        assertEquals("Final completed response", messages[0].text)
    }

    @Test
    fun clearHistory_removesAllPersistedMessages() = runTest {
        repository.saveMessage(
            ChatMessage(id = "msg-1", text = "Hi", participant = Participant.USER)
        )
        repository.saveMessage(
            ChatMessage(id = "msg-2", text = "Hello", participant = Participant.ASSISTANT)
        )
        assertEquals(2, repository.getMessages().first().size)

        repository.clearHistory()
        val messages = repository.getMessages().first()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun entityConversion_preservesAllPropertiesAccurately() {
        val original = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "Test text",
            participant = Participant.USER,
            isPending = false,
            timestamp = 123456789L,
        )

        val entity = original.toEntity()
        assertEquals(original.id, entity.id)
        assertEquals(original.text, entity.text)
        assertEquals("USER", entity.participant)
        assertEquals(original.timestamp, entity.timestamp)

        val convertedBack = entity.toChatMessage()
        assertEquals(original.id, convertedBack.id)
        assertEquals(original.text, convertedBack.text)
        assertEquals(original.participant, convertedBack.participant)
        assertFalse(convertedBack.isPending)
        assertEquals(original.timestamp, convertedBack.timestamp)
    }
}
