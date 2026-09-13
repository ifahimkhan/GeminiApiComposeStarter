package com.fahim.geminiApiComposeStarter.ui.chat

import org.junit.Assert.*
import org.junit.Test

class PromptIntentTest {
    @Test fun explicitImageRequestsUseImageGeneration() {
        listOf("Create an image of a cat", "Can you please generate a watercolor picture of a forest?",
            "Draw a cat", "Make me a logo for a bakery", "Design a poster for a concert").forEach {
            assertTrue(it, requestsImageCreation(it))
        }
    }

    @Test fun imageDiscussionAndOrdinaryChatRemainText() {
        listOf("Explain image generation", "Write a prompt to create an image", "Create a Kotlin function",
            "How do I draw a cat?", "Don't generate an image").forEach {
            assertFalse(it, requestsImageCreation(it))
        }
    }
}
