package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testChatScreenDisplaysInitialState() {
        composeTestRule.onNodeWithText("Gemini AI Chat").assertExists()
        composeTestRule.onNodeWithText("Response will be displayed here!").assertExists()
    }

    @Test
    fun testUserCanTypePrompt() {
        composeTestRule.onNodeWithText("Enter your prompt here").performTextInput("Hello Gemini")
        composeTestRule.onNodeWithText("Hello Gemini").assertExists()
    }
}
