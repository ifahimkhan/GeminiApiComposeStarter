package com.n149.geminichat

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.n149.geminichat.ui.chat.ChatScreen
import com.n149.geminichat.ui.theme.GeminiChatTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [ChatScreen].
 * Uses Hilt test rules to inject a real (test-scoped) dependency graph.
 */
@HiltAndroidTest
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ChatScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun setContent() {
        composeRule.setContent {
            GeminiChatTheme {
                ChatScreen(
                    windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))
                )
            }
        }
    }

    @Test
    fun appBar_displaysTitleCorrectly() {
        setContent()
        composeRule.onNodeWithText("N149 · Gemini Chat").assertIsDisplayed()
    }

    @Test
    fun inputField_isDisplayed() {
        setContent()
        composeRule.onNodeWithText("Ask Gemini anything…").assertIsDisplayed()
    }

    @Test
    fun sendButton_disabledWhenInputEmpty() {
        setContent()
        composeRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun sendButton_enabledWhenInputNotEmpty() {
        setContent()
        composeRule.onNodeWithText("Ask Gemini anything…").performTextInput("Hello")
        composeRule.onNodeWithContentDescription("Send").assertIsEnabled()
    }

    @Test
    fun voiceButton_isDisplayed() {
        setContent()
        composeRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
    }

    @Test
    fun clearHistoryMenuOption_isAccessible() {
        setContent()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Clear history").assertIsDisplayed()
    }
}
