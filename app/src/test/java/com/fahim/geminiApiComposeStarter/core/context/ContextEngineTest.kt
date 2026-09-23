package com.fahim.geminiApiComposeStarter.core.context

import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole
import com.fahim.geminiApiComposeStarter.model.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextEngineTest {

    private val contextEngine = ContextEngine(maxHistoryWindow = 5)

    @Test
    fun `buildContext limits history to maxHistoryWindow`() {
        val messages = (1..10).map { i ->
            ChatMessage(
                id = "$i",
                role = if (i % 2 == 1) MessageRole.USER else MessageRole.GEMINI,
                content = "Message $i",
            )
        }

        val context = contextEngine.buildContext(
            history = messages,
            newPrompt = "New Question",
            mode = StudyMode.CHAT,
        )

        assertEquals(5, context.recentMessages.size)
        assertEquals("Message 6", context.recentMessages.first().content)
        assertEquals("Message 10", context.recentMessages.last().content)
    }

    @Test
    fun `buildContext wraps prompt with mode specific instructions`() {
        val context = contextEngine.buildContext(
            history = emptyList(),
            newPrompt = "Binary Trees",
            mode = StudyMode.EXPLAIN,
        )

        assertTrue(context.effectivePrompt.contains("Explain the following topic thoroughly"))
        assertTrue(context.effectivePrompt.contains("Binary Trees"))
        assertTrue(context.systemInstruction.contains("💡 Core Concept"))
    }

    @Test
    fun `buildContext includes saved notes when provided`() {
        val savedNotes = listOf(
            ChatMessage(role = MessageRole.GEMINI, content = "Key formula: E=mc^2", isSaved = true)
        )

        val context = contextEngine.buildContext(
            history = emptyList(),
            newPrompt = "Physics question",
            mode = StudyMode.CHAT,
            relevantSavedNotes = savedNotes,
        )

        assertTrue(context.systemInstruction.contains("Student's saved reference notes:"))
        assertTrue(context.systemInstruction.contains("E=mc^2"))
    }

    @Test
    fun `buildContext includes user memories and required system instructions when provided`() {
        val memories = listOf(
            com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity(
                category = "PROFILE",
                content = "Name: Shubh",
            ),
            com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity(
                category = "PREFERENCE",
                content = "I prefer concise answers",
            ),
        )

        val context = contextEngine.buildContext(
            history = emptyList(),
            newPrompt = "What is my name?",
            mode = StudyMode.CHAT,
            userMemories = memories,
        )

        assertTrue(context.systemInstruction.contains("Information in LOCAL USER MEMORY represents facts explicitly stored by the user"))
        assertTrue(context.systemInstruction.contains("USER MEMORY:"))
        assertTrue(context.systemInstruction.contains("Name: Shubh"))
        assertTrue(context.systemInstruction.contains("I prefer concise answers"))
        assertTrue(context.memoryContext.contains("Name: Shubh"))
    }
}
