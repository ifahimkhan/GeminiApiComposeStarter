package com.fahim.geminiApiComposeStarter.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechChunkingTest {

    @Test
    fun `short text stays in one chunk`() {
        assertEquals(listOf("Hello world"), chunkForSpeech("Hello world", maxLength = 100))
    }

    @Test
    fun `blank text gives no chunks`() {
        assertTrue(chunkForSpeech("   \n ", maxLength = 100).isEmpty())
    }

    @Test
    fun `long text is split at spaces and no chunk exceeds the limit`() {
        val text = "one two three four five six seven eight nine ten"

        val chunks = chunkForSpeech(text, maxLength = 15)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 15 })
        // Nothing is lost and no word is cut in half.
        assertEquals(text.split(" "), chunks.flatMap { it.split(" ") })
    }

    @Test
    fun `line breaks are preferred as cut points`() {
        val chunks = chunkForSpeech("first line\nsecond line", maxLength = 15)

        assertEquals(listOf("first line", "second line"), chunks)
    }

    @Test
    fun `a word longer than the limit is cut hard`() {
        val chunks = chunkForSpeech("abcdefghij", maxLength = 4)

        assertEquals(listOf("abcd", "efgh", "ij"), chunks)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a non positive limit is rejected`() {
        chunkForSpeech("text", maxLength = 0)
    }
}
