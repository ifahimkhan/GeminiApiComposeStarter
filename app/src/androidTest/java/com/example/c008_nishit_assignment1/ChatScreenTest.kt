package com.example.c008_nishit_assignment1

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.c008_nishit_assignment1.ui.chat.ChatBubble
import com.example.c008_nishit_assignment1.ui.chat.ChatMessage
import com.example.c008_nishit_assignment1.ui.theme.C008_Nishit_Assignment1Theme
import org.junit.Rule
import org.junit.Test

/**
 * UI Instrumentation test for ChatScreen composables.
 */
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatBubble_displaysUserMessageText() {
        val message = ChatMessage(id = 1, text = "Hello Gemini Test", isUser = true)

        composeTestRule.setContent {
            C008_Nishit_Assignment1Theme {
                ChatBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("You").assertIsDisplayed()
    }

    @Test
    fun chatBubble_displaysGeminiMessageText() {
        val message = ChatMessage(id = 2, text = "I am Gemini response", isUser = false)

        composeTestRule.setContent {
            C008_Nishit_Assignment1Theme {
                ChatBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("I am Gemini response").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini").assertIsDisplayed()
    }
}
