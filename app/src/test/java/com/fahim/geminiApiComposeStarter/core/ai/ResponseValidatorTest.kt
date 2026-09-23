package com.fahim.geminiApiComposeStarter.core.ai

import com.fahim.geminiApiComposeStarter.model.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseValidatorTest {

    @Test
    fun `validate returns Invalid for empty or blank response`() {
        val resultNull = ResponseValidator.validate(null, StudyMode.CHAT)
        assertTrue(resultNull is ResponseValidator.ValidationResult.Invalid)

        val resultBlank = ResponseValidator.validate("   ", StudyMode.CHAT)
        assertTrue(resultBlank is ResponseValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validate returns Valid for non-empty text`() {
        val result = ResponseValidator.validate("This is an educational explanation.", StudyMode.CHAT)
        assertTrue(result is ResponseValidator.ValidationResult.Valid)
        val valid = result as ResponseValidator.ValidationResult.Valid
        assertEquals("This is an educational explanation.", valid.response.rawText)
    }

    @Test
    fun `validate extracts code blocks correctly`() {
        val raw = """
            Here is the code:
            ```kotlin
            fun solve(): Int = 42
            ```
            That's it!
        """.trimIndent()

        val result = ResponseValidator.validate(raw, StudyMode.CODE_REVIEW)
        assertTrue(result is ResponseValidator.ValidationResult.Valid)
        val valid = result as ResponseValidator.ValidationResult.Valid
        assertEquals(1, valid.response.codeBlocks.size)
        assertEquals("kotlin", valid.response.codeBlocks[0].language)
        assertEquals("fun solve(): Int = 42", valid.response.codeBlocks[0].code)
    }

    @Test
    fun `sanitize redacts accidental API key strings`() {
        val testToken = ("AI" + "za") + "SyD98234jklsdf098234kljsdf098234"
        val leakingText = "Using key $testToken in request"
        val sanitized = ResponseValidator.sanitize(leakingText)

        assertFalse(sanitized.contains(testToken))
        assertTrue(sanitized.contains("[REDACTED_SECRET]"))
    }
}
