package com.fahim.geminiApiComposeStarter.ui.text

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoldMarkdownTest {

    @Test
    fun plainText_containsNoBoldStyles() {
        val input = "This is regular plain text without bolding."
        val annotated = input.toBoldAnnotatedString()

        assertEquals(input, annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
    }

    @Test
    fun singleBoldPattern_isParsedCorrectly() {
        val input = "This is **important** information."
        val annotated = input.toBoldAnnotatedString()

        assertEquals("This is important information.", annotated.text)
        assertEquals(1, annotated.spanStyles.size)

        val span = annotated.spanStyles[0]
        assertEquals(FontWeight.Bold, span.item.fontWeight)
        assertEquals(8, span.start)
        assertEquals(17, span.end)
    }

    @Test
    fun multipleBoldPatterns_areAllParsed() {
        val input = "**Step 1**: Open the app. **Step 2**: Enter a prompt."
        val annotated = input.toBoldAnnotatedString()

        assertEquals("Step 1: Open the app. Step 2: Enter a prompt.", annotated.text)
        assertEquals(2, annotated.spanStyles.size)

        val firstSpan = annotated.spanStyles[0]
        assertEquals(FontWeight.Bold, firstSpan.item.fontWeight)
        assertEquals(0, firstSpan.start)
        assertEquals(6, firstSpan.end)

        val secondSpan = annotated.spanStyles[1]
        assertEquals(FontWeight.Bold, secondSpan.item.fontWeight)
        assertEquals(22, secondSpan.start)
        assertEquals(28, secondSpan.end)
    }

    @Test
    fun emptyString_returnsEmptyAnnotatedString() {
        val input = ""
        val annotated = input.toBoldAnnotatedString()

        assertEquals("", annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
    }
}
