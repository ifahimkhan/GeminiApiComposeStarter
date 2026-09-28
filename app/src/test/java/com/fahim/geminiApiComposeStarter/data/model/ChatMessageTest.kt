package com.fahim.geminiApiComposeStarter.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageTest {

    @Test
    fun chatMessage_defaultValues_areValid() {
        val message = ChatMessage(
            text = "Test prompt",
            sender = MessageSender.USER,
        )

        assertEquals(0L, message.id)
        assertEquals("Test prompt", message.text)
        assertEquals(MessageSender.USER, message.sender)
        assertEquals(MessageStatus.SUCCESS, message.status)
        assertTrue(message.timestamp > 0)
    }

    @Test
    fun chatMessage_loadingStatus_isPreserved() {
        val botMessage = ChatMessage(
            text = "",
            sender = MessageSender.GEMINI,
            status = MessageStatus.LOADING,
        )

        assertEquals(MessageSender.GEMINI, botMessage.sender)
        assertEquals(MessageStatus.LOADING, botMessage.status)
        assertTrue(botMessage.text.isEmpty())
    }

    @Test
    fun chatMessage_errorStatus_isPreserved() {
        val errorMessage = ChatMessage(
            text = "API rate limit exceeded",
            sender = MessageSender.GEMINI,
            status = MessageStatus.ERROR,
        )

        assertEquals(MessageSender.GEMINI, errorMessage.sender)
        assertEquals(MessageStatus.ERROR, errorMessage.status)
        assertEquals("API rate limit exceeded", errorMessage.text)
    }

    @Test
    fun chatMessage_uniqueId_assignment() {
        val msg1 = ChatMessage(id = 101L, text = "One", sender = MessageSender.USER)
        val msg2 = ChatMessage(id = 102L, text = "Two", sender = MessageSender.GEMINI)

        assertEquals(101L, msg1.id)
        assertEquals(102L, msg2.id)
        assertTrue(msg1.id != msg2.id)
    }
}
