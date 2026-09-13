package com.example.c020_harsh_assignment1

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import com.example.c020_harsh_assignment1.ui.chat.ChatScreen
import com.example.c020_harsh_assignment1.ui.chat.ChatUiState
import com.example.c020_harsh_assignment1.ui.theme.C020_Harsh_Assignment1Theme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun studentInfoIsDisplayed() {
        composeTestRule.setContent {
            C020_Harsh_Assignment1Theme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onStyleChange = {},
                    windowWidthSizeClass = WindowWidthSizeClass.Compact
                )
            }
        }

        composeTestRule.onNodeWithText("Harsh Baniya • C020").assertIsDisplayed()
    }

    @Test
    fun inputAndButtonsAreVisible() {
        composeTestRule.setContent {
            C020_Harsh_Assignment1Theme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onStyleChange = {},
                    windowWidthSizeClass = WindowWidthSizeClass.Compact
                )
            }
        }

        composeTestRule.onNodeWithText("Enter your prompt").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice Input").assertIsDisplayed()
    }

    @Test
    fun responseStylesAreVisible() {
        composeTestRule.setContent {
            C020_Harsh_Assignment1Theme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onClearHistory = {},
                    onStyleChange = {},
                    windowWidthSizeClass = WindowWidthSizeClass.Compact
                )
            }
        }

        composeTestRule.onNodeWithText("Simple").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detailed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Technical").assertIsDisplayed()
    }
}
