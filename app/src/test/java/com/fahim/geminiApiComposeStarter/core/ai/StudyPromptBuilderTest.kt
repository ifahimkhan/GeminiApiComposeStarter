package com.fahim.geminiApiComposeStarter.core.ai

import com.fahim.geminiApiComposeStarter.model.StudyMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyPromptBuilderTest {

    @Test
    fun `all 8 study modes have distinct non-empty system instructions`() {
        for (mode in StudyMode.entries) {
            val instruction = StudyPromptBuilder.getSystemInstruction(mode)
            assertFalse("System instruction for $mode must not be blank", instruction.isBlank())
            assertTrue("System instruction for $mode must have substantial guidance", instruction.length > 30)
        }
    }

    @Test
    fun `wrapPrompt preserves prompt content across modes`() {
        val topic = "Dynamic Programming"
        for (mode in StudyMode.entries) {
            val wrapped = StudyPromptBuilder.wrapPrompt(topic, mode)
            assertTrue("Wrapped prompt for $mode must contain topic", wrapped.contains(topic))
        }
    }

    @Test
    fun `explain mode instruction contains academic breakdown headers`() {
        val instruction = StudyPromptBuilder.getSystemInstruction(StudyMode.EXPLAIN)
        assertTrue(instruction.contains("Core Concept"))
        assertTrue(instruction.contains("Intuitive Explanation"))
        assertTrue(instruction.contains("Key Takeaways"))
        assertTrue(instruction.contains("Real-World Example"))
        assertTrue(instruction.contains("Common Pitfall"))
    }
}
