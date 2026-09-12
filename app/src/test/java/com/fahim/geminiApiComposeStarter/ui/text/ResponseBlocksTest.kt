package com.fahim.geminiApiComposeStarter.ui.text

import org.junit.Assert.*
import org.junit.Test

class ResponseBlocksTest {
    @Test fun proseRemainsOutsideCodeContainers() {
        val blocks = parseResponseBlocks("Before\n```kotlin\nval x = 1\n```\nAfter")
        assertEquals(3, blocks.size)
        assertNull(blocks[0].language)
        assertEquals("kotlin", blocks[1].language)
        assertEquals("val x = 1", blocks[1].text)
        assertNull(blocks[2].language)
    }
    @Test fun unfinishedFenceKeepsCodeLiteral() {
        val block = parseResponseBlocks("```\n  **not bold**").single()
        assertEquals("", block.language)
        assertEquals("  **not bold**", block.text)
    }
}
