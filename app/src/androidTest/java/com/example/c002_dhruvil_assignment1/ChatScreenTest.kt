package com.example.c002_dhruvil_assignment1

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.c002_dhruvil_assignment1.ui.chat.ChatScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testChatScreenDisplaysTitleAndStudentHeader() {
        composeTestRule.setContent {
            ChatScreen()
        }

        composeTestRule.onNodeWithText("Gemini AI Assistant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dhruvil Kothari - C002").assertIsDisplayed()
    }
}
