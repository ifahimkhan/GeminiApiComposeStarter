package com.fahim.geminiApiComposeStarter.core.ai

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class IntentRouterTest {

    private lateinit var router: IntentRouter

    @Before
    fun setup() {
        router = IntentRouter()
    }

    @Test
    fun `classifies natural memory commands correctly`() {
        val intent1 = router.classify("Remember that my name is Shubh")
        assertEquals(IntentType.MEMORY_COMMAND, intent1.type)

        val intent2 = router.classify("What do you remember about me?")
        assertEquals(IntentType.MEMORY_COMMAND, intent2.type)

        val intent3 = router.classify("Forget that I study mechanical engineering")
        assertEquals(IntentType.MEMORY_COMMAND, intent3.type)

        val intent4 = router.classify("Forget everything you remember about me")
        assertEquals(IntentType.MEMORY_COMMAND, intent4.type)
    }

    @Test
    fun `classifies calculation commands correctly`() {
        val intent1 = router.classify("Calculate 25 * 4 + 10")
        assertEquals(IntentType.CALCULATION, intent1.type)

        val intent2 = router.classify("mean of 10, 20, 30, 40")
        assertEquals(IntentType.CALCULATION, intent2.type)

        val intent3 = router.classify("convert 100 celsius to fahrenheit")
        assertEquals(IntentType.CALCULATION, intent3.type)

        val intent4 = router.classify("25 * 4")
        assertEquals(IntentType.CALCULATION, intent4.type)
    }

    @Test
    fun `classifies document queries correctly when document attached`() {
        val intent = router.classify("Summarize the key sections", hasAttachedDocument = true)
        assertEquals(IntentType.DOCUMENT_QUERY, intent.type)
    }

    @Test
    fun `classifies study workflow commands correctly`() {
        val intent1 = router.classify("Quiz me on operating systems")
        assertEquals(IntentType.STUDY_WORKFLOW, intent1.type)

        val intent2 = router.classify("Create flashcards on computer networks")
        assertEquals(IntentType.STUDY_WORKFLOW, intent2.type)
    }

    @Test
    fun `classifies code commands correctly`() {
        val intent = router.classify("Review this code:\n```kotlin\nval x = 1\n```")
        assertEquals(IntentType.CODE_ANALYSIS, intent.type)
    }

    @Test
    fun `classifies general chat when no specific trigger matches`() {
        val intent = router.classify("Tell me about the history of computing")
        assertEquals(IntentType.GENERAL_CONVERSATION, intent.type)
    }
}
