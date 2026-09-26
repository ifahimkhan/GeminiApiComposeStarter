package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/** Exercises the real Activity, ViewModel, Room and credential failure path together. */
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun keyboardKeepsSendVisible() {
        compose.onAllNodesWithText("Generate")[0].performClick()
        compose.onNodeWithTag("prompt").performClick()
        compose.waitUntil(10_000) {
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithTag("send").assertIsDisplayed()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val folder = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        java.io.File(folder, "keyboard-test.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun localConversationSurvivesActivityRecreation() {
        assumeTrue("This smoke test must not make a live API call", BuildConfig.GEMINI_API_KEY.isBlank())
        compose.onAllNodesWithText("Generate")[0].performClick()
        compose.onNodeWithTag("prompt").assertIsDisplayed().performTextReplacement("Local persistence check")
        compose.onNodeWithTag("send").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Couldn't send").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Retry").assertIsDisplayed()
        val app = compose.activity.application as ChatApplication
        val conversation = runBlocking { app.store.conversations("Local persistence check").first().first() }
        assertEquals("Local persistence check", runBlocking { app.store.messages(conversation.id).first().first().text })
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Local persistence check").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Local persistence check").assertIsDisplayed().performClick()
        compose.onNodeWithText("Retry").assertIsDisplayed()
        // Remove only the conversation created by this test, preserving any pre-existing data.
        app.database.openHelper.writableDatabase.execSQL("DELETE FROM conversations WHERE id = ?", arrayOf(conversation.id))
    }
}
