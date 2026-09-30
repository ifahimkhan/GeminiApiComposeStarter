package com.fahim.geminiApiComposeStarter.data.local

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageEntityTest {

    @Test
    fun roundTripConversion_preservesAllFields() {
        val original = ChatMessage(
            id = "msg-123",
            text = "Hello Room Database",
            participant = Participant.USER,
            timestamp = 1700000000000L,
        )

        val entity = ChatMessageEntity.fromChatMessage(original)
        assertEquals("msg-123", entity.id)
        assertEquals("Hello Room Database", entity.text)
        assertEquals("USER", entity.participant)
        assertEquals(1700000000000L, entity.timestamp)

        val convertedBack = entity.toChatMessage()
        assertEquals(original, convertedBack)
    }

    @Test
    fun assistantMessageConversion_preservesRole() {
        val original = ChatMessage(
            id = "msg-456",
            text = "Hello from Gemini",
            participant = Participant.ASSISTANT,
            timestamp = 1700000001000L,
        )

        val entity = ChatMessageEntity.fromChatMessage(original)
        assertEquals("ASSISTANT", entity.participant)

        val convertedBack = entity.toChatMessage()
        assertEquals(Participant.ASSISTANT, convertedBack.participant)
    }
}
