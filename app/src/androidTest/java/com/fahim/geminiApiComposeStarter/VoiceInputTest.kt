package com.fahim.geminiApiComposeStarter

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VoiceInputTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun setup() { Intents.init() }
    @After fun teardown() { Intents.release() }

    @Test fun recognizerResultPopulatesThePrompt() {
        Intents.intending(hasAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)).respondWith(
            ActivityResult(Activity.RESULT_OK, Intent().putStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS, arrayListOf("Explain Android state hoisting"),
            )),
        )
        compose.waitForIdle()
        compose.onNodeWithText("Voice").performClick()
        Intents.intended(hasAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
        compose.onNodeWithTag("prompt").assertTextContains("Explain Android state hoisting")
    }

    @Test fun canceledRecognitionLeavesPromptUnchanged() {
        Intents.intending(hasAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)).respondWith(
            ActivityResult(Activity.RESULT_CANCELED, null),
        )
        compose.waitForIdle()
        compose.onNodeWithText("Voice").performClick()
        Intents.intended(hasAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
        compose.onNodeWithTag("prompt").assertTextContains("Message Gemini")
    }
}
