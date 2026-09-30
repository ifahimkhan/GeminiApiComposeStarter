package com.fahim.geminiApiComposeStarter.ui.text

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Markdown parser in [BoldMarkdown.kt] (Phase 5a).
 */
class BoldMarkdownTest {

    @Test
    fun asteriskBullets_renderAsBulletPoints() {
        val input = "* First bullet\n* Second bullet"
        val annotated = input.toMarkdownAnnotatedString()
        assertEquals("• First bullet\n• Second bullet", annotated.text)
    }

    @Test
    fun dashBullets_renderAsBulletPoints() {
        val input = "- Alpha\n- Beta"
        val annotated = input.toMarkdownAnnotatedString()
        assertEquals("• Alpha\n• Beta", annotated.text)
    }

    @Test
    fun mathAsterisks_notTurnedIntoItalics() {
        val input = "Calculate 2 * 3 * 4 = 24"
        val annotated = input.toMarkdownAnnotatedString()
        assertEquals("Calculate 2 * 3 * 4 = 24", annotated.text)
        val hasItalic = annotated.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        assertFalse("Math expression asterisks must not be parsed as italic", hasItalic)
    }

    @Test
    fun fencedCodeBlocks_renderAsMonospace_withoutMarkdownMangling() {
        val input = "```kotlin\nval sum = 2 * 3\nval bold = **not bold**\n```"
        val annotated = input.toMarkdownAnnotatedString()
        assertTrue("Content must be preserved verbatim", annotated.text.contains("val sum = 2 * 3"))
        assertTrue("Content must be preserved verbatim", annotated.text.contains("val bold = **not bold**"))

        // Inside code block, font family must be Monospace
        val monospaceSpans = annotated.spanStyles.filter { it.item.fontFamily == FontFamily.Monospace }
        assertTrue("Code block lines must have Monospace font style", monospaceSpans.isNotEmpty())

        // Inside code block, **not bold** must NOT be converted to bold SpanStyle
        val boldSpans = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Fenced code blocks must not parse markdown bold", boldSpans.isEmpty())
    }

    @Test
    fun boldAndItalic_doNotCollide() {
        val input = "Normal **bold text** and *italic text* and ***both bold and italic***."
        val annotated = input.toMarkdownAnnotatedString()
        assertEquals("Normal bold text and italic text and both bold and italic.", annotated.text)

        val boldOnlySpan = annotated.spanStyles.find {
            it.item.fontWeight == FontWeight.Bold && it.item.fontStyle != FontStyle.Italic
        }
        val italicOnlySpan = annotated.spanStyles.find {
            it.item.fontStyle == FontStyle.Italic && it.item.fontWeight != FontWeight.Bold
        }
        val bothSpan = annotated.spanStyles.find {
            it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic
        }

        assertTrue("Bold span must exist", boldOnlySpan != null)
        assertTrue("Italic span must exist", italicOnlySpan != null)
        assertTrue("Combined bold+italic span must exist", bothSpan != null)
    }

    @Test
    fun inlineCode_rendersWithMonospaceFont() {
        val input = "Use `val key = 123` in code"
        val annotated = input.toMarkdownAnnotatedString()
        assertEquals("Use val key = 123 in code", annotated.text)
        val codeSpan = annotated.spanStyles.find { it.item.fontFamily == FontFamily.Monospace }
        assertTrue("Inline code must have Monospace font family", codeSpan != null)
    }
}
