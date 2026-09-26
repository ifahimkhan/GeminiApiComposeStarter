package com.example.c210_utkarshtanna_mad_assignment1

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun textFormatter_formatsBoldCorrectly() {
        val input = "Hello **world** test"
        val formatted = TextFormatter.formatToAnnotatedString(input)
        assertEquals("Hello world test", formatted.text)
    }
}
