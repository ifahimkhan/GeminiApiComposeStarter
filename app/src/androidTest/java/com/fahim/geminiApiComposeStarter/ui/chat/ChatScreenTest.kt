package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.ui.chat.components.LOADING_TAG
import com.fahim.geminiApiComposeStarter.ui.chat.components.MODEL_BUBBLE_TAG
import com.fahim.geminiApiComposeStarter.ui.chat.components.USER_BUBBLE_TAG
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val messages = listOf(
        ChatMessage(1, Role.USER, "Hello Gemini", 1),
        ChatMessage(2, Role.MODEL, "Hello! How can I help?", 2),
    )

    private fun setScreen(state: ChatUiState, actions: ChatActions = ChatActions(), layout: ChatLayout = ChatLayout.COMPACT) {
        composeRule.setContent {
            GeminiApiComposeStarterTheme(dynamicColor = false) {
                ChatScreen(state = state, actions = actions, layout = layout)
            }
        }
    }

    @Test
    fun emptyConversation_showsEmptyState() {
        setScreen(ChatUiState())
        composeRule.onNodeWithTag(EMPTY_STATE_TAG).assertIsDisplayed()
    }

    @Test
    fun messages_areRenderedAsUserAndModelBubbles() {
        setScreen(ChatUiState(messages = messages))
        composeRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
        composeRule.onNodeWithText("Hello! How can I help?").assertIsDisplayed()
        composeRule.onAllNodesWithTag(USER_BUBBLE_TAG).assertCountEquals(1)
        composeRule.onAllNodesWithTag(MODEL_BUBBLE_TAG).assertCountEquals(1)
    }

    @Test
    fun loading_showsProgress_andDisablesSend() {
        setScreen(ChatUiState(messages = messages, isLoading = true))
        composeRule.onNodeWithTag(LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SEND_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun typingAndSending_invokesCallbacks() {
        val typed = mutableListOf<String>()
        var sendCount = 0
        setScreen(
            ChatUiState(),
            ChatActions(onPromptChange = { typed += it }, onSend = { sendCount++ }),
        )

        composeRule.onNodeWithTag(PROMPT_FIELD_TAG).performTextInput("Hi")
        composeRule.onNodeWithTag(SEND_BUTTON_TAG).performClick()

        assertTrue(typed.contains("Hi"))
        assertEquals(1, sendCount)
    }

    @Test
    fun micButton_invokesVoiceCallback() {
        var micClicks = 0
        setScreen(ChatUiState(), ChatActions(onMicClick = { micClicks++ }))
        composeRule.onNodeWithTag(MIC_BUTTON_TAG).performClick()
        assertEquals(1, micClicks)
    }

    @Test
    fun emptyPromptError_isShown() {
        setScreen(ChatUiState(promptError = PromptError.EMPTY))
        composeRule.onNodeWithText("Field cannot be empty").assertIsDisplayed()
    }

    @Test
    fun requestError_showsSnackbarWithRetry() {
        var retried = 0
        setScreen(
            ChatUiState(messages = messages, error = ChatError.Request("Network down"), retryPrompt = "Hello Gemini"),
            ChatActions(onRetry = { retried++ }),
        )
        composeRule.onNodeWithText("Request failed: Network down").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()
        assertEquals(1, retried)
    }

    @Test
    fun expandedLayout_showsSettingsPane() {
        setScreen(ChatUiState(messages = messages), layout = ChatLayout.EXPANDED)
        composeRule.onNodeWithText("Response style").assertIsDisplayed()
    }
}
