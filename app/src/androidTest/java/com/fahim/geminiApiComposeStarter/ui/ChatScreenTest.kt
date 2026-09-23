package com.fahim.geminiApiComposeStarter.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatTestTags
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(state: ChatUiState) {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    onPromptChange = {},
                    onSend = {},
                    onDismissError = {},
                    onClearHistory = {},
                )
            }
        }
    }

    @Test
    fun chatScreen_emptyState_isDisplayed() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun chatScreen_inputField_exists() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.INPUT_FIELD).assertIsDisplayed()
    }

    @Test
    fun chatScreen_sendButton_exists() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsDisplayed()
    }

    @Test
    fun chatScreen_voiceButton_exists() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.VOICE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun chatScreen_modeButton_exists() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.MODE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun chatScreen_imageButton_exists() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.IMAGE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun chatScreen_savedNotesButton_exists() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.SAVED_NOTES_BUTTON).assertIsDisplayed()
    }

    @Test
    fun chatScreen_insightsButton_exists() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.INSIGHTS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun chatScreen_userCanEnterText() {
        setScreen(ChatUiState())

        composeTestRule.onNodeWithTag(ChatTestTags.INPUT_FIELD)
            .performTextInput("Hello Gemini")

        composeTestRule.onNodeWithText("Hello Gemini").assertIsDisplayed()
    }

    @Test
    fun chatScreen_messagesAreDisplayed() {
        val messages = listOf(
            ChatMessage(id = "1", role = MessageRole.USER, content = "Hello!"),
            ChatMessage(id = "2", role = MessageRole.GEMINI, content = "Hi there!"),
        )
        setScreen(ChatUiState(messages = messages))

        composeTestRule.onNodeWithText("Hello!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi there!").assertIsDisplayed()
    }

    @Test
    fun chatScreen_loadingState_showsIndicator() {
        setScreen(ChatUiState(
            messages = listOf(
                ChatMessage(id = "1", role = MessageRole.USER, content = "Hello"),
            ),
            isLoading = true,
        ))

        composeTestRule.onNodeWithTag(ChatTestTags.LOADING_INDICATOR).assertIsDisplayed()
    }

    @Test
    fun chatScreen_sendButton_disabledWhenLoading() {
        setScreen(ChatUiState(isLoading = true))

        composeTestRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun chatScreen_sendButton_disabledWhenEmpty() {
        setScreen(ChatUiState(prompt = ""))

        composeTestRule.onNodeWithTag(ChatTestTags.SEND_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun chatScreen_messageList_showsWithMessages() {
        val messages = listOf(
            ChatMessage(id = "1", role = MessageRole.USER, content = "Test message"),
        )
        setScreen(ChatUiState(messages = messages))

        composeTestRule.onNodeWithTag(ChatTestTags.MESSAGE_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ChatTestTags.EMPTY_STATE).assertDoesNotExist()
    }

    @Test
    fun chatScreen_roleLabelDisplayed() {
        val messages = listOf(
            ChatMessage(id = "1", role = MessageRole.USER, content = "Hello"),
            ChatMessage(id = "2", role = MessageRole.GEMINI, content = "Hi!"),
        )
        setScreen(ChatUiState(messages = messages))

        composeTestRule.onNodeWithText("You").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini").assertIsDisplayed()
    }
}
